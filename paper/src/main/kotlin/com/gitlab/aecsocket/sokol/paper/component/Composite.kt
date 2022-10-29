package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.makeEntity
import org.spongepowered.configurate.ConfigurationNode
import kotlin.collections.HashMap

data class Composite(
    private val sokol: Sokol,
    val children: MutableMap<String, SokolEntity>,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("composite")
    }

    override val componentType get() = Composite::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound().apply {
        children.forEach { (key, entity) ->
            set(key) { makeEntity(sokol, entity) }
        }
    }

    override fun write(node: ConfigurationNode) {
        children.forEach { (key, entity) ->
            node.node(key).set(entity)
        }
    }

    class Profile(
        private val sokol: Sokol,
        val children: Map<String, EntityProfile>
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound().run { Composite(sokol,
            associate { (key, tag) -> tag.asCompound().run {
                val blueprint = children[key]?.let { profile ->
                    sokol.persistence.readBlueprintByProfile(profile, this)
                } ?: sokol.persistence.readBlueprint(this)
                key to sokol.engine.buildEntity(blueprint)
            } }.toMutableMap()
        ) }

        override fun read(node: ConfigurationNode) = Composite(sokol,
            node.childrenMap().map { (key, child) ->
                val blueprint = children[key.toString()]?.let { profile ->
                    deserializeBlueprintByProfile(sokol, profile, child)
                } ?: child.force()
                key.toString() to sokol.engine.buildEntity(blueprint)
            }.toMap().toMutableMap()
        )

        override fun readEmpty() = Composite(sokol, HashMap())
    }

    class Type(private val sokol: Sokol) : ComponentType {
        override val key get() = Key

        override fun createProfile(node: ConfigurationNode) = Profile(sokol,
            node.force<HashMap<String, EntityProfile>>())
    }

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
    return getOr(entity)?.children?.get(key)
}

fun ComponentMapper<Composite>.child(entity: SokolEntity, path: Iterable<String>): SokolEntity? {
    var current = entity
    path.forEach { key -> current = getOr(current)?.children?.get(key) ?: return null }
    return current
}

fun ComponentMapper<Composite>.child(entity: SokolEntity, vararg path: String) = child(entity, path.asList())

fun ComponentMapper<Composite>.allChildren(entity: SokolEntity): Map<List<String>, SokolEntity> {
    getOr(entity)?.let { composite ->
        val result = HashMap<List<String>, SokolEntity>()
        composite.children.forEach { (key, child) ->
            result[listOf(key)] = child
            allChildren(child).forEach { (subKeys, subChild) ->
                result[listOf(key) + subKeys] = subChild
            }
        }
        return result
    } ?: return emptyMap()
}

fun ComponentMapper<Composite>.forEachChild(entity: SokolEntity, action: (Map.Entry<String, SokolEntity>) -> Unit) {
    getOr(entity)?.children?.forEach(action)
}

fun ComponentMapper<Composite>.forward(entity: SokolEntity, event: SokolEvent) {
    forEachChild(entity) { (_, child) ->
        child.call(event)
    }
}
