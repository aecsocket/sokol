package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI

data class Takeable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("takeable")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = Takeable::class
    override val key get() = Key

    object Profile : SimpleComponentProfile {
        override val componentType get() = Takeable::class

        override fun createEmpty() = ComponentBlueprint { Takeable(this) }
    }
}

@All(Takeable::class, Removable::class, PositionRead::class /* if we have a world presence */)
@Before(EntityCallbacksSystem::class, RemovableTarget::class)
class TakeableSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val TakeAsItem = Takeable.Key.with("take_as_item")
    }

    private val mRemovable = ids.mapper<Removable>()

    @Subscribe
    fun on(event: EntityCallbacksSystem.Construct, entity: SokolEntity) {
        val removable = mRemovable.get(entity)

        event.callback(TakeAsItem) {
            if (removable.removed) return@callback false
            removable.remove()

            // todo

            true
        }
    }
}
