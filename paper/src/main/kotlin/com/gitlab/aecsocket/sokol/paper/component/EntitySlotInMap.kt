package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class EntitySlotInMap(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("entity_slot_in_map")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { EntitySlotInMapSystem(it) }
            ctx.system { EntitySlotInMapForwardSystem(it) }
        }
    }

    override val componentType get() = EntitySlotInMap::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val shape: Shape,
        val childKey: String = ContainerMap.DefaultKey,
        val allows: Boolean = true
    ) : SimpleComponentProfile<EntitySlotInMap> {
        override val componentType get() = EntitySlotInMap::class

        override fun createEmpty() = ComponentBlueprint { EntitySlotInMap(this) }
    }
}

fun EntitySlotInMap.childOf(container: ContainerMap) = container.child(profile.childKey)

@All(EntitySlotInMap::class, ContainerMap::class)
@None(EntitySlot::class)
class EntitySlotInMapSystem(ids: ComponentIdAccess) : SokolSystem {
    private val compositeMutator = CompositeMutator(ids)
    private val mEntitySlotInMap = ids.mapper<EntitySlotInMap>()
    private val mEntitySlot = ids.mapper<EntitySlot>()
    private val mContainerMap = ids.mapper<ContainerMap>()

    object Construct : SokolEvent

    @Subscribe
    fun on(event: Construct, entity: SokolEntity) {
        val entitySlotInMap = mEntitySlotInMap.get(entity).profile
        val containerMap = mContainerMap.get(entity)

        val childKey = entitySlotInMap.childKey
        mEntitySlot.set(entity, object : EntitySlot {
            override val shape get() = entitySlotInMap.shape

            override fun full(): Boolean {
                return containerMap.contains(childKey)
            }

            override fun allows(): Boolean {
                return entitySlotInMap.allows
            }

            override fun attach(child: SokolEntity) {
                containerMap.attach(childKey, child)
                compositeMutator.attach(entity, child) { containerMap.detach(childKey) }
            }
        })
    }
}

@Before(EntitySlotTarget::class)
class EntitySlotInMapForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, EntitySlotInMapSystem.Construct)
    }
}
