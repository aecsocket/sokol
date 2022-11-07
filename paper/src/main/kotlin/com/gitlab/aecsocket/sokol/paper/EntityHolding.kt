package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.sokol.core.SokolEntity
import com.gitlab.aecsocket.sokol.core.SokolEvent
import com.jme3.bullet.collision.shapes.CollisionShape
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.lang.ref.WeakReference
import java.util.UUID

class HoldState(
    val player: Player,
    var entity: SokolEntity,
    val operation: HoldOperation,
    var mob: WeakReference<Entity>?,
    val raiseHandLock: PlayerLockInstance,
) {
    var frozen: Boolean = false
    var drawShape: CollisionShape? = null
    var drawSlotShapes: Boolean = false
}

interface HoldOperation {
    val canRelease: Boolean
}

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

    fun start(player: AlexandriaPlayer, entity: SokolEntity, operation: HoldOperation, mob: Entity?): HoldState {
        val state = HoldState(
            player.handle,
            entity,
            operation,
            mob?.let { WeakReference(it) },
            player.acquireLock(PlayerLock.RaiseHand),
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
