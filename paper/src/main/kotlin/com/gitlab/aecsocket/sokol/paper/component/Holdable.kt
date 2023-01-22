package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.entity.Player

object Holdable : SimplePersistentComponent {
    override val componentType get() = Holdable::class
    override val key = SokolAPI.key("holdable")
    val Type = ComponentType.singletonComponent(key, this)

    fun init(ctx: Sokol.InitContext) {
        ctx.persistentComponent(Type)
        ctx.system { HoldableSystem(ctx.sokol, it).init(ctx) }
    }
}

class HoldableSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Stop = Holdable.key.with("stop")
    }

    private val mHoldable = ids.mapper<Holdable>()
    private val mHeld = ids.mapper<Held>()

    internal fun init(ctx: Sokol.InitContext): HoldableSystem {
        ctx.components.callbacks.apply {
            callback(Stop, ::stop)
        }
        return this
    }

    private fun stop(entity: SokolEntity, player: Player): Boolean {
        if (!mHoldable.has(entity)) return false

        // Held is not a guarantee, since it can be added/removed over the lifetime of an entity
        // and we only set up the callbacks on construct
        val (hold) = mHeld.getOr(entity) ?: return false
        if (player !== hold.player) return false

        sokol.holding.stop(hold)
        return true
    }
}
