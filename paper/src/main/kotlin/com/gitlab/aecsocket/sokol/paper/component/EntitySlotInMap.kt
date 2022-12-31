package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class EntitySlotInMap(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("entity_slot_in_map")
        val Type = ComponentType.deserializing(Key, Profile::class)
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

@All(EntitySlotInMap::class, ContainerMap::class)
@None(EntitySlot::class)
@Before(EntitySlotTarget::class)
class EntitySlotInMapSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mEntitySlotInMap = ids.mapper<EntitySlotInMap>()
    private val mEntitySlot = ids.mapper<EntitySlot>()
    private val mContainerMap = ids.mapper<ContainerMap>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val entitySlotInMap = mEntitySlotInMap.get(entity).profile
        val containerMap = mContainerMap.get(entity)

        mEntitySlot.set(entity, object : EntitySlot {
            override val shape get() = entitySlotInMap.shape

            override fun full(): Boolean {
                return containerMap.contains(entitySlotInMap.childKey)
            }

            override fun allows(): Boolean {
                return entitySlotInMap.allows
            }

            override fun attach(child: SokolEntity) {
                containerMap.attach(entitySlotInMap.childKey, child)
                entity.addEntity(child)
            }
        })
    }
}
