package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.GameMode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class Holdable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Holdable::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val settings: MoveHoldSettings
    ) : SimpleComponentProfile {
        override val componentType get() = Holdable::class

        override fun createEmpty() = ComponentBlueprint { Holdable(this) }
    }
}

enum class MoveHoldState(val canRelease: Boolean) {
    ALLOW           (true),
    DISALLOW        (false),
    ALLOW_ATTACH    (true),
    DISALLOW_ATTACH (true)
}

@ConfigSerializable
data class MoveHoldSettings(
    val holdTransform: Transform = Transform.Identity,
    val holdDistance: Double = 0.0,
    val snapDistance: Double = 0.0,
    val allowFreePlace: Boolean = true
)

data class MoveHoldOperation(
    val settings: MoveHoldSettings
) : HoldOperation {
    var state: MoveHoldState = MoveHoldState.DISALLOW

    override val canRelease get() = state.canRelease
}

@All(Holdable::class, InputCallbacks::class)
@Before(InputCallbacksSystem::class)
class HoldableSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val StartMove = Holdable.Key.with("start_move")
        val Stop = Holdable.Key.with("stop")
    }

    private val mHoldable = ids.mapper<Holdable>()
    private val mInputCallbacks = ids.mapper<InputCallbacks>()
    private val mHeld = ids.mapper<Held>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val holdable = mHoldable.get(entity).profile
        val inputCallbacks = mInputCallbacks.get(entity)

        inputCallbacks.callback(StartMove) { input ->
            if (mHeld.has(entity)) // this entity is already held
                return@callback false

            sokol.holding.start(input.player.alexandria, entity, MoveHoldOperation(holdable.settings))
            true
        }

        inputCallbacks.callback(Stop) { input ->
            // Held is not a guarantee, since it can be added/removed over the lifetime of an entity
            // and we only set up the callbacks on construct
            val (hold) = mHeld.getOr(entity) ?: return@callback false
            if (input.player !== hold.player) return@callback false

            sokol.holding.stop(hold)
            true
        }
    }
}

@All(Holdable::class, IsItem::class, AsMob::class)
class HoldableItemSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mHoldable = ids.mapper<Holdable>()
    private val mIsItem = ids.mapper<IsItem>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntity) {
        val holdable = mHoldable.get(entity).profile
        val isItem = mIsItem.get(entity)
        if (!event.isRightClick || !event.isShiftClick) return

        val player = event.player
        event.cancel()

        if (player.gameMode != GameMode.CREATIVE) {
            isItem.item.subtract()
        }

        player.closeInventory()
        val mobEntity = sokol.persistence.blueprintOf(entity).create()
        sokol.hoster.hostMob(mobEntity, player.eyeLocation)
        sokol.holding.start(player.alexandria, mobEntity, MoveHoldOperation(holdable.settings))
    }
}
