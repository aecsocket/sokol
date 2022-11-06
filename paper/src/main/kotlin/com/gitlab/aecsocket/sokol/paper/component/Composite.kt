package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode
import kotlin.collections.HashMap

data class Composite(
    private val sokol: Sokol,
    private val children: MutableMap<String, Delta<SokolEntity?>>,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("composite")
    }

    override val componentType get() = Composite::class
    override val key get() = Key

    // even though this composite's children might not change,
    // the child state might
    override val dirty get() = true

    fun children(): Map<String, SokolEntity> = children
        .mapNotNull { (key, delta) -> delta.value?.let { key to it } }
        .associate { it }

    operator fun contains(key: String) = children[key]?.value != null

    fun child(key: String) = children[key]?.value

    fun attach(key: String, value: SokolEntity) {
        if (children.contains(key))
            throw IllegalStateException("Entity already exists in slot $key")
        children[key] = Delta(value, true)
    }

    fun detach(key: String): SokolEntity? {
        return children[key]?.let { delta ->
            val old = delta.value
            delta.value = null
            delta.dirty()
            old
        }
    }

    override fun write(ctx: NBTTagContext) = ctx.makeCompound().apply {
        children.forEach { (key, delta) ->
            delta.value?.let { set(key) { sokol.persistence.writeEntity(it) } }
        }
    }

    override fun writeDelta(tag: NBTTag): NBTTag {
        val compound = tag.asCompound()
        children.forEach { (key, delta) ->
            val child = delta.value
            if (delta.dirty) {
                child?.let {
                    compound.set(key) { sokol.persistence.writeEntity(child) }
                } ?: run {
                    compound.remove(key)
                }
            } else {
                child?.let {
                    compound[key]?.let {
                        sokol.persistence.writeEntityDeltas(child, it.asCompound())
                    }
                }
            }
        }
        return tag
    }

    override fun write(node: ConfigurationNode) {
        children.forEach { (key, entity) ->
            entity.value?.let { node.node(key).set(it) }
        }
    }

    class Profile(
        private val sokol: Sokol,
        val children: Map<String, EntityProfile>
    ) : ComponentProfile {
        override fun read(tag: NBTTag): Composite {
            val compound = tag.asCompound()

            val result = HashMap<String, Delta<SokolEntity?>>()
            compound.forEach { (key, child) ->
                val blueprint = children[key]?.let { profile ->
                    sokol.persistence.readBlueprintByProfile(child.asCompound(), profile)
                } ?: sokol.persistence.readBlueprint(child.asCompound())
                result[key] = Delta(sokol.engine.buildEntity(blueprint))
            }

            children.forEach { (key, profile) ->
                if (!result.contains(key)) {
                    result[key] = Delta(sokol.engine.buildEntity(sokol.engine.emptyBlueprint(profile)))
                }
            }

            return Composite(sokol, result)
        }

        override fun read(node: ConfigurationNode): Composite {
            val result = HashMap<String, Delta<SokolEntity?>>()
            node.childrenMap().forEach { (key, child) ->
                val blueprint = children[key.toString()]?.let { profile ->
                    deserializeBlueprintByProfile(sokol, profile, child)
                } ?: child.force()
                result[key.toString()] = Delta(sokol.engine.buildEntity(blueprint))
            }

            children.forEach { (key, profile) ->
                if (!result.contains(key)) {
                    result[key] = Delta(sokol.engine.buildEntity(sokol.engine.emptyBlueprint(profile)))
                }
            }

            return Composite(sokol, result)
        }

        override fun readEmpty() = Composite(sokol, HashMap())
    }

    class Type(private val sokol: Sokol) : ComponentType {
        override val key get() = Key

        override fun createProfile(node: ConfigurationNode) = Profile(sokol,
            node.force<HashMap<String, EntityProfile>>())
    }
}

data class CompositeChild(
    val key: String,
    val parent: SokolEntity,
    val path: CompositePath,
) : SokolComponent {
    override val componentType get() = CompositeChild::class
}

class CompositeSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mComposite = mappers.componentMapper<Composite>()
    private val mCompositeChild = mappers.componentMapper<CompositeChild>()

    @Subscribe
    fun on(event: AttachTo, entity: SokolEntity) {
        val path = compositePathOf(
            (mCompositeChild.getOr(event.parent)?.path ?: emptyCompositePath()) + event.key
        )
        mCompositeChild.set(entity, CompositeChild(event.key, event.parent, path))
        mComposite.forEachChild(entity) { (key, child) ->
            child.call(AttachTo(key, entity, event.fresh))
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        mComposite.forEachChild(entity) { (key, child) ->
            child.call(AttachTo(key, entity, false))
        }
    }

    data class AttachTo(
        val key: String,
        val parent: SokolEntity,
        val fresh: Boolean,
    ) : SokolEvent

    object TreeMutate : SokolEvent
}

interface CompositePath : List<String>

private object EmptyListIterator : ListIterator<String> {
    override fun hasNext() = false
    override fun hasPrevious() = false
    override fun next() = throw NoSuchElementException()
    override fun nextIndex() = 1
    override fun previous() = throw NoSuchElementException()
    override fun previousIndex() = -1
}

private object EmptyCompositePath : CompositePath {
    override val size get() = 0
    override fun isEmpty() = true
    override fun contains(element: String) = false
    override fun containsAll(elements: Collection<String>) = elements.isEmpty()
    override fun get(index: Int) = throw IndexOutOfBoundsException(index)
    override fun indexOf(element: String) = -1
    override fun lastIndexOf(element: String) = -1
    override fun listIterator() = EmptyListIterator
    override fun listIterator(index: Int) = EmptyListIterator
    override fun iterator() = EmptyListIterator
    override fun subList(fromIndex: Int, toIndex: Int) = emptyList<String>()
    override fun toString() = "[]"
}

private class CompositePathImpl(private val path: List<String>) : CompositePath {
    override val size get() = path.size
    override fun isEmpty() = path.isEmpty()
    override fun contains(element: String) = path.contains(element)
    override fun containsAll(elements: Collection<String>) = path.containsAll(elements)
    override fun get(index: Int) = path[index]
    override fun indexOf(element: String) = path.indexOf(element)
    override fun lastIndexOf(element: String) = path.lastIndexOf(element)
    override fun listIterator() = path.listIterator()
    override fun listIterator(index: Int) = path.listIterator(index)
    override fun iterator() = path.iterator()
    override fun subList(fromIndex: Int, toIndex: Int) = path.subList(fromIndex, toIndex)
    override fun toString() = path.toString()
}

fun emptyCompositePath(): CompositePath = EmptyCompositePath

fun compositePathOf(path: List<String>): CompositePath = when (path.size) {
    0 -> EmptyCompositePath
    else -> CompositePathImpl(path)
}

fun compositePathOf(vararg path: String): CompositePath = compositePathOf(path.asList())

fun ComponentMapper<Composite>.child(entity: SokolEntity, key: String): SokolEntity? {
    return getOr(entity)?.child(key)
}

fun ComponentMapper<Composite>.child(entity: SokolEntity, path: Iterable<String>): SokolEntity? {
    var current = entity
    path.forEach { key -> current = getOr(current)?.child(key) ?: return null }
    return current
}

fun ComponentMapper<Composite>.child(entity: SokolEntity, vararg path: String) = child(entity, path.asList())

fun ComponentMapper<Composite>.allChildren(entity: SokolEntity): Map<List<String>, SokolEntity> {
    getOr(entity)?.let { composite ->
        val result = HashMap<List<String>, SokolEntity>()
        composite.children().forEach { (key, child) ->
            result[listOf(key)] = child
            allChildren(child).forEach { (subKeys, subChild) ->
                result[listOf(key) + subKeys] = subChild
            }
        }
        return result
    } ?: return emptyMap()
}

fun ComponentMapper<Composite>.forEachChild(entity: SokolEntity, action: (Map.Entry<String, SokolEntity>) -> Unit) {
    getOr(entity)?.children()?.forEach(action)
}

fun ComponentMapper<Composite>.forAllEntities(entity: SokolEntity, action: (SokolEntity) -> Unit) {
    action(entity)
    forEachChild(entity) { (_, child) ->
        forAllEntities(child, action)
    }
}

fun <E : SokolEvent> ComponentMapper<Composite>.forward(entity: SokolEntity, event: E): E {
    forEachChild(entity) { (_, child) ->
        child.call(event)
    }
    return event
}

fun <E : SokolEvent> ComponentMapper<Composite>.forwardAll(entity: SokolEntity, event: E): E {
    forAllEntities(entity) { target ->
        target.call(event)
    }
    return event
}
