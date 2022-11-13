package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode

data class ContainerMap(
    private val sokol: Sokol,
    private val children: MutableMap<String, Delta<SokolEntity?>>
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("container_map")
    }

    override val componentType get() = ContainerMap::class
    override val key get() = Key
    override val dirty get() = true

    fun children() = children
        .mapNotNull { (key, dEntity) -> dEntity.value?.let { key to it } }
        .associate { it }

    operator fun contains(key: String) = children[key]?.value != null

    fun child(key: String) = children[key]?.value

    fun attach(key: String, value: SokolEntity) {
        if (contains(key))
            throw IllegalStateException("Entity already exists in slot $key")
        children[key] = Delta(value, true)
    }

    fun detach(key: String): SokolEntity? {
        val delta = children[key] ?: return null
        val old = delta.value
        delta.value = null
        delta.dirty()
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
    ) : ComponentProfile {
        override val componentType get() = ContainerMap::class

        private val mIsChild = sokol.engine.mapper<IsChild>()
        private val mInTag = sokol.engine.mapper<InTag>()

        override fun read(tag: NBTTag, entity: SokolEntity, space: SokolSpaceAccess): PersistentComponent {
            val compound = tag.asCompound()
            val root = mIsChild.getOr(entity)?.root ?: entity

            val result = HashMap<String, Delta<SokolEntity?>>()

            compound.forEach { (key, childTag) ->
                val childCompound = childTag.asCompound()
                val blueprint = children[key]?.let { profile ->
                    sokol.persistence.readProfileBlueprint(childCompound, profile)
                } ?: sokol.persistence.readBlueprint(childCompound)

                result[key] = Delta(blueprint.createIn(space.withEntityFunction { child ->
                    mIsChild.set(entity, IsChild(child, root))
                    mInTag.set(entity, InTag(childCompound))
                }))
            }

            children.forEach { (key, profile) ->
                if (result.contains(key)) return@forEach
                result[key] = Delta(sokol.persistence.emptyBlueprint(profile).createIn(space.withEntityFunction { child ->
                    mIsChild.set(child, IsChild(entity, root))
                    mInTag.remove(child)
                }))
            }

            return ContainerMap(sokol, result)
        }

        override fun deserialize(node: ConfigurationNode, entity: SokolEntity, space: SokolSpaceAccess): PersistentComponent {
            val result = HashMap<String, Delta<SokolEntity?>>()
            val root = mIsChild.getOr(entity)?.root ?: entity

            node.childrenMap().forEach { (key, child) ->
                val blueprint = (children[key.toString()]?.let { profile ->
                    sokol.persistence.deserializeProfileBlueprint(child, profile)
                } ?: child.force<EntityBlueprint>())
                    .with(mIsChild) { IsChild(entity, root) }
                result[key.toString()] = Delta(blueprint.createIn(space))
            }

            children.forEach { (key, profile) ->
                if (result.contains(key)) return@forEach
                result[key] = Delta(sokol.persistence.emptyBlueprint(profile)
                    .with(mIsChild) { IsChild(entity, root) }
                    .createIn(space))
            }

            return ContainerMap(sokol, result)
        }

        override fun createEmpty(entity: SokolEntity, space: SokolSpaceAccess) = ContainerMap(sokol, HashMap())
    }

    class Type(private val sokol: Sokol) : ComponentType {
        override val key get() = Key

        override fun createProfile(node: ConfigurationNode) = Profile(sokol,
            node.force<HashMap<String, EntityProfile>>())
    }
}