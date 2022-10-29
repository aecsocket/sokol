package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.jme3.bullet.collision.shapes.CollisionShape
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.lang.ref.WeakReference
import java.util.UUID

class EntityHolding(
    private val sokol: Sokol
) : PlayerFeature<EntityHolding.PlayerData> {
    @ConfigSerializable
    data class HoldSettings(
        val placeTransform: Transform,
        val holdDistance: Double,
        val snapDistance: Double,
    )

    data class State(
        val player: Player,
        val mobHost: WeakReference<Entity>,
        val raiseHandLock: PlayerLockInstance,
        var transform: Transform,

        var frozen: Boolean = false,
        var drawShape: CollisionShape? = null,
    )

    inner class PlayerData(
        val player: AlexandriaPlayer,
        var state: State? = null
    ) : PlayerFeature.PlayerData {
        override fun dispose() {
            state?.let { stopInternal(player, it, false) }
            state = null
        }
    }

    private val _mobToState = HashMap<UUID, State>()
    val mobToState: Map<UUID, State> get() = _mobToState

    override fun createFor(player: AlexandriaPlayer) = PlayerData(player)

    private fun stopInternal(player: AlexandriaPlayer, state: State, releaseLock: Boolean = true) {
        state.mobHost.get()?.let { _mobToState.remove(it.uniqueId) }
        if (releaseLock) {
            player.releaseLock(state.raiseHandLock)
        }
    }

    fun start(player: AlexandriaPlayer, state: State) {
        val data = player.featureData(this)
        data.state?.let { stopInternal(player, it) }
        data.state = state
        state.mobHost.get()?.let { _mobToState[it.uniqueId] = state }
    }

    fun start(player: AlexandriaPlayer, mobHost: Entity, transform: Transform): State {
        val state = State(
            player.handle,
            WeakReference(mobHost),
            player.acquireLock(PlayerLock.RaiseHand),
            transform,
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
}

val AlexandriaPlayer.entityHolding: EntityHolding.State?
    get() = featureData(SokolAPI.entityHolding).state
