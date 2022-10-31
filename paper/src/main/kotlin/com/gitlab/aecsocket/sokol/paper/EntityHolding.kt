package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.sokol.core.SokolEntity
import com.gitlab.aecsocket.sokol.core.SokolEvent
import com.gitlab.aecsocket.sokol.paper.component.CompositePath
import com.jme3.bullet.collision.shapes.CollisionShape
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.lang.ref.WeakReference
import java.util.UUID

enum class HoldPlaceState(val valid: Boolean) {
    ALLOW           (true),
    DISALLOW        (false),
    ALLOW_ATTACH    (true),
    DISALLOW_ATTACH (true)
}

data class HoldAttach(
    val entity: SokolEntity,
    val path: CompositePath,
)

data class HoldState(
    val player: Player,
    var entity: SokolEntity,
    val raiseHandLock: PlayerLockInstance,
    var transform: Transform,
    var mob: WeakReference<Entity>?,

    var placing: HoldPlaceState = HoldPlaceState.DISALLOW,
    var attachTo: HoldAttach? = null,
    var frozen: Boolean = false,
    var drawShape: CollisionShape? = null,
    var drawSlotShapes: Boolean = false,
)

class EntityHolding internal constructor() : PlayerFeature<EntityHolding.PlayerData> {
    inner class PlayerData(
        val player: AlexandriaPlayer,
        var state: HoldState? = null
    ) : PlayerFeature.PlayerData {
        override fun dispose() {
            state?.let { stopInternal(player, it, false) }
        }
    }

    private val _heldBy = HashMap<UUID, HoldState>()
    val heldBy: Map<UUID, HoldState> get() = _heldBy

    override fun createFor(player: AlexandriaPlayer) = PlayerData(player)

    private fun stopInternal(player: AlexandriaPlayer, state: HoldState, releaseLock: Boolean = true) {
        state.entity.call(ChangeHoldState(player.handle, state, false))
        state.mob?.get()?.let { _heldBy.remove(it.uniqueId) }

        if (releaseLock) {
            player.releaseLock(state.raiseHandLock)
        }
    }

    fun start(player: AlexandriaPlayer, state: HoldState) {
        val data = player.featureData(this)
        data.state?.let { stopInternal(player, it) }
        data.state = state

        state.mob?.get()?.let { _heldBy[it.uniqueId] = state }
        state.entity.call(ChangeHoldState(player.handle, state, true))
    }

    fun start(player: AlexandriaPlayer, entity: SokolEntity, transform: Transform, mob: Entity?): HoldState {
        val state = HoldState(
            player.handle,
            entity,
            player.acquireLock(PlayerLock.RaiseHand),
            transform,
            WeakReference(mob),
        )
        start(player, state)
        return state
    }

    fun stop(player: AlexandriaPlayer) {
        val data = player.featureData(this)
        data.state?.let { state ->
            stopInternal(player, state)
            data.state = null
        }
    }

    data class ChangeHoldState(
        val player: Player,
        val state: HoldState,
        val holding: Boolean
    ) : SokolEvent
}

val AlexandriaPlayer.heldEntity: HoldState?
    get() = featureData(SokolAPI.entityHolding).state
