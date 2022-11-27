package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.glossa.core.force
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import kotlin.reflect.KClass

interface PersistentComponent : SokolComponent {
    val key: Key
    val dirty: Boolean

    fun clean()

    fun write(ctx: NBTTagContext): NBTTag?

    fun writeDelta(tag: NBTTag): NBTTag

    fun serialize(node: ConfigurationNode)
}

interface SimplePersistentComponent : PersistentComponent {
    override val dirty get() = false

    override fun clean() {}

    override fun write(ctx: NBTTagContext) = null

    override fun writeDelta(tag: NBTTag) = tag

    override fun serialize(node: ConfigurationNode) {}
}

fun interface ComponentBlueprint<out C : SokolComponent> {
    fun create(entity: SokolEntity): C
}

interface ComponentProfile {
    val componentType: KClass<out SokolComponent>

    fun read(tag: NBTTag): ComponentBlueprint<*>

    fun deserialize(node: ConfigurationNode): ComponentBlueprint<*>

    fun createEmpty(): ComponentBlueprint<*>
}

interface SimpleComponentProfile : ComponentProfile {
    override fun read(tag: NBTTag) = createEmpty()

    override fun deserialize(node: ConfigurationNode) = createEmpty()
}

class EntityBlueprint internal constructor(
    private val engine: SokolEngine,
    var flags: Int,
    private val functions: MutableList<(SokolEntity) -> Unit> = ArrayList()
) {
    fun push(function: (SokolEntity) -> Unit): EntityBlueprint {
        functions.add(0, function)
        return this
    }

    fun <C : SokolComponent> pushSet(mapper: ComponentMapper<C>, component: ComponentBlueprint<C>): EntityBlueprint {
        push { mapper.set(it, component.create(it)) }
        return this
    }

    fun <C : SokolComponent> pushSet(profile: ComponentProfile, component: ComponentBlueprint<C>): EntityBlueprint {
        @Suppress("UNCHECKED_CAST")
        val mapper = engine.mapper(profile.componentType) as ComponentMapper<C>
        push { mapper.set(it, component.create(it)) }
        return this
    }

    fun pushRemove(mapper: ComponentMapper<*>): EntityBlueprint {
        push { mapper.remove(it) }
        return this
    }

    fun create(): SokolEntity {
        val entity = engine.newEntity(flags)
        functions.forEach { it(entity) }
        return entity
    }

    fun copyOf() = EntityBlueprint(engine, flags, functions.toMutableList())
}

fun SokolEngine.newBlueprint(flags: Int = 0) = EntityBlueprint(this, flags)

fun EntityBlueprint.addInto(space: SokolSpace) = space.addEntity(create())

fun Iterable<EntityBlueprint>.addAllInto(space: SokolSpace) {
    forEach { space.addEntity(it.create()) }
}

open class EntityProfile(
    val components: Map<Key, ComponentProfile>
)

class KeyedEntityProfile(
    override val id: String,
    components: Map<Key, ComponentProfile>
) : EntityProfile(components), Keyed

data class Profiled(val profile: KeyedEntityProfile) : SokolComponent {
    override val componentType get() = Profiled::class
}

val SokolEntity.id get() = component<Profiled>().profile.id

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

                override fun createEmpty() = ComponentBlueprint { component }
            }
        }

        fun singletonProfile(key: Key, profile: ComponentProfile) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode) = profile
        }

        inline fun <reified P : ComponentProfile> deserializing(key: Key) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode) = node.force<P>()
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
