package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleDelayed
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.PositionWrite
import com.gitlab.aecsocket.sokol.paper.component.SupplierEntityAccess
import com.gitlab.aecsocket.sokol.paper.component.hovered
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
            stop(player)
        }
    }

    private val _mobToState = HashMap<UUID, State>()
    val mobToState: Map<UUID, State> get() = _mobToState

    private lateinit var mSupplierEntityAccess: ComponentMapper<SupplierEntityAccess>
    private lateinit var mPosition: ComponentMapper<PositionWrite>

    override fun createFor(player: AlexandriaPlayer) = PlayerData(player)

    internal fun enable() {
        mSupplierEntityAccess = sokol.engine.componentMapper()
        mPosition = sokol.engine.componentMapper()

        sokol.entityResolver.inputHandler { event ->
            val player = event.player
            val axPlayer = player.alexandria

            axPlayer.entityHolding?.mobHost?.get()?.let { mob ->
                sokol.useMob(mob) { entity ->
                    entity.call(event)
                }
            }
        }
    }

    private fun stopInternal(player: AlexandriaPlayer, state: State) {
        state.mobHost.get()?.let { _mobToState.remove(it.uniqueId) }
        player.releaseLock(state.raiseHandLock)
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
