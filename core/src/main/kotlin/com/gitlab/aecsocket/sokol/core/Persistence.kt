package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.glossa.core.force
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import kotlin.reflect.KClass

interface PersistentComponent : SokolComponent {
    val key: Key
    val dirty: Boolean

    fun write(ctx: NBTTagContext): NBTTag

    fun writeDelta(tag: NBTTag): NBTTag

    fun serialize(node: ConfigurationNode)
}

interface SimplePersistentComponent : PersistentComponent {
    override val dirty get() = false

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun writeDelta(tag: NBTTag) = tag

    override fun serialize(node: ConfigurationNode) {}
}

interface ComponentProfile {
    val componentType: KClass<out SokolComponent>

    fun read(space: SokolSpaceAccess, tag: NBTTag): PersistentComponent

    fun deserialize(space: SokolSpaceAccess, node: ConfigurationNode): PersistentComponent

    fun createEmpty(): PersistentComponent
}

interface SimpleComponentProfile : ComponentProfile {
    override fun read(space: SokolSpaceAccess, tag: NBTTag) = createEmpty()

    override fun deserialize(space: SokolSpaceAccess, node: ConfigurationNode) = createEmpty()
}

open class EntityProfile(
    val components: Map<Key, ComponentProfile>
)

class KeyedEntityProfile(
    override val id: String,
    components: Map<Key, ComponentProfile>
) : EntityProfile(components), Keyed

data class Profiled(val id: String) : SokolComponent {
    override val componentType get() = Profiled::class
}

data class InTag(val tag: CompoundNBTTag) : SokolComponent {
    override val componentType get() = InTag::class
}

interface ComponentType {
    val key: Key

    fun createProfile(node: ConfigurationNode): ComponentProfile

    companion object {
        fun singletonComponent(key: Key, component: PersistentComponent) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode) = object : SimpleComponentProfile {
                override val componentType get() = component.componentType

                override fun createEmpty() = component
            }
        }

        fun singletonProfile(key: Key, profile: ComponentProfile) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode): ComponentProfile {
                return profile
            }
        }

        inline fun <reified P : ComponentProfile> deserializing(key: Key) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode): P {
                return node.force<P>()
            }
        }
    }
}

object WriteEvent : SokolEvent

fun SokolSpace.write() = call(WriteEvent)

@All(InTag::class)
class PersistenceSystem(
    private val sokol: SokolAPI,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mInTag = ids.mapper<InTag>()

    @Subscribe
    fun on(event: WriteEvent, entity: SokolEntity) {
        val inTag = mInTag.get(entity)
        sokol.persistence.writeEntityDelta(entity, inTag.tag)
    }
}

fun ComponentIdAccess.blueprintOf(entity: SokolEntity): EntityBlueprint {
    return EntityBlueprint(entity.flags, entity.allComponents().map { component ->
        mapper(component.componentType) to ComponentFactory { component }
    })
}
