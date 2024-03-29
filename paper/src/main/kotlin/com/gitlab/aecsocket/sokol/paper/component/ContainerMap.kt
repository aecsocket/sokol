package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.ConfigurationNode

data class ContainerMap(
    private val sokol: Sokol,
    val children: MutableMap<String, Delta<SokolEntity?>>
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("container_map")
        const val DefaultKey = "_"

        val CompositeKey = Key.with("composite")

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type(ctx.sokol))
        }
    }

    override val componentType get() = ContainerMap::class
    override val key get() = Key
    override val dirty get() = true

    override fun clean() {
        children.forEach { (_, delta) -> delta.clean() }
    }

    fun children() = children
        .mapNotNull { (key, dEntity) -> dEntity.value?.let { key to it } }
        .associate { it }

    operator fun contains(key: String) = children[key]?.value != null

    fun child(key: String) = children[key]?.value

    fun attach(key: String, value: SokolEntity) {
        if (contains(key))
            throw IllegalArgumentException("Entity in slot $key already exists")
        children[key] = Delta(value, true)
    }

    fun detach(key: String): SokolEntity? {
        val delta = children[key] ?: return null
        val old = delta.value
        delta.set(null)
        return old
    }

    override fun write(ctx: NBTTagContext): CompoundNBTTag {
        val compound = ctx.makeCompound()

        children.forEach { (key, dChild) ->
            dChild.value?.let { compound.set(key) { sokol.persistence.writeEntity(it) } }
        }

        return compound
    }

    override fun writeDelta(tag: NBTTag): NBTTag {
        val compound = tag.asCompound()

        children.forEach { (key, dChild) ->
            val child = dChild.value
            if (dChild.dirty) {
                child?.let {
                    compound.set(key) { sokol.persistence.writeEntity(child) }
                } ?: compound.remove(key)
            } else {
                child?.let {
                    compound[key]?.let { sokol.persistence.writeEntityDelta(child, it.asCompound()) }
                }
            }
        }

        return tag
    }

    override fun serialize(node: ConfigurationNode) {
        children.forEach { (key, dChild) ->
            dChild.value?.let { node.node(key).set(it) }
        }
    }

    class Profile(
        private val sokol: Sokol,
        val children: Map<String, EntityProfile>
    ) : ComponentProfile<ContainerMap> {
        override val componentType get() = ContainerMap::class

        private val mComposite = sokol.engine.mapper<Composite>()
        private val mIsChild = sokol.engine.mapper<IsChild>()
        private val mInTag = sokol.engine.mapper<InTag>()

        private fun blueprintOf(blueprints: Map<String, EntityBlueprint>) = ComponentBlueprint { entity ->
            val composite = mComposite.getOrSet(entity) { Composite(entity.engine) }
            val root = mIsChild.root(entity)

            val children = HashMap<String, Delta<SokolEntity?>>()
            val component = ContainerMap(sokol, children)
            blueprints.forEach { (key, blueprint) ->
                children[key] = Delta(blueprint
                    .pushSet(mIsChild) { IsChild(entity) { component.detach(key) } }
                    .create())
            }

            composite.entityProvider(CompositeKey) { component.children().values }
            component
        }

        override fun read(tag: NBTTag): ComponentBlueprint<ContainerMap> {
            val compound = tag.asCompound()

            val blueprints = HashMap<String, EntityBlueprint>()

            compound.forEach { (key, childTag) ->
                val childCompound = childTag.asCompound()
                val blueprint = children[key]?.let { profile ->
                    sokol.persistence.readProfiledBlueprint(childCompound, profile)
                } ?: sokol.persistence.readBlueprint(childCompound)

                blueprints[key] = blueprint
                    .pushSet(mInTag) { InTag(childCompound) }
            }

            children.forEach { (key, profile) ->
                if (blueprints.contains(key)) return@forEach
                blueprints[key] = sokol.persistence.emptyBlueprint(profile)
                    .pushRemove(mInTag)
            }

            return blueprintOf(blueprints)
        }

        override fun deserialize(node: ConfigurationNode): ComponentBlueprint<ContainerMap> {
            val blueprints = HashMap<String, EntityBlueprint>()

            node.childrenMap().forEach { (key, child) ->
                val blueprint = (children[key.toString()]?.let { profile ->
                    sokol.persistence.deserializeProfiledBlueprint(child, profile)
                } ?: child.force<EntityBlueprint>())

                blueprints[key.toString()] = blueprint
            }

            children.forEach { (key, profile) ->
                if (blueprints.contains(key)) return@forEach
                blueprints[key] = sokol.persistence.emptyBlueprint(profile)
            }

            return blueprintOf(blueprints)
        }

        override fun createEmpty() = blueprintOf(children
            .map { (key, profile) -> key to sokol.persistence.emptyBlueprint(profile) }
            .associate { it })
    }

    class Type(private val sokol: Sokol) : ComponentType<ContainerMap> {
        override val key get() = Key

        override fun createProfile(node: ConfigurationNode) = Profile(sokol,
            node.force<HashMap<String, EntityProfile>>())
    }
}