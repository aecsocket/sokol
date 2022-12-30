package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*

object Holdable : SimplePersistentComponent {
    override val componentType get() = Holdable::class
    override val key = SokolAPI.key("holdable")
    val Type = ComponentType.singletonComponent(key, this)
}

@All(Holdable::class, InputCallbacks::class)
@Before(InputCallbacksSystem::class)
class HoldableInputsSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Stop = Holdable.key.with("stop")
    }

    private val mInputCallbacks = ids.mapper<InputCallbacks>()
    private val mHeld = ids.mapper<Held>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val inputCallbacks = mInputCallbacks.get(entity)

        inputCallbacks.callback(Stop) { player ->
            // Held is not a guarantee, since it can be added/removed over the lifetime of an entity
            // and we only set up the callbacks on construct
            val (hold) = mHeld.getOr(entity) ?: return@callback false
            if (player !== hold.player) return@callback false

            sokol.holding.stop(hold)
            true
        }
    }
}
