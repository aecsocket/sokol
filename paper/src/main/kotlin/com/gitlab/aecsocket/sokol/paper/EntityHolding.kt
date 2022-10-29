package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.input.InputMapper
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.PositionWrite
import com.gitlab.aecsocket.sokol.paper.component.SupplierEntityAccess
import com.jme3.bullet.collision.shapes.CollisionShape
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable

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
        var raiseHandLock: PlayerLockInstance,
        var transform: Transform,

        var frozen: Boolean = false,
        var drawShape: CollisionShape? = null,
    )

    inner class PlayerData(
        var state: State? = null
    ) : PlayerFeature.PlayerData

    enum class InputAction {
        TAKE,
        RELEASE
    }

    @ConfigSerializable
    data class Settings(
        val inputs: InputMapper<List<InputAction>> = InputMapper.builder<List<InputAction>>().build()
    )

    lateinit var settings: Settings
        private set

    private lateinit var mSupplierEntityAccess: ComponentMapper<SupplierEntityAccess>
    private lateinit var mPosition: ComponentMapper<PositionWrite>

    override fun createFor(player: AlexandriaPlayer) = PlayerData()

    internal fun enable() {
        mSupplierEntityAccess = sokol.engine.componentMapper()
        mPosition = sokol.engine.componentMapper()

        sokol.entityResolver.inputHandler { (input, player, cancel) ->
            val axPlayer = player.alexandria
            val actions = settings.inputs.getForPlayer(input, player) ?: return@inputHandler
            for (action in actions) {
                when (action) {
                    InputAction.TAKE -> {
                        cancel()
                        break
                    }
                    InputAction.RELEASE -> {
                        cancel()
                        stop(axPlayer)
                        break
                    }
                }
            }
        }
    }

    internal fun load() {
        settings = sokol.settings.entityHolding
    }

    private fun stopInternal(player: AlexandriaPlayer, state: State) {
        player.releaseLock(state.raiseHandLock)
    }

    fun start(player: AlexandriaPlayer, state: State) {
        val data = player.featureData(this)
        data.state?.let { stopInternal(player, it) }
        data.state = state
    }

    fun start(player: AlexandriaPlayer, transform: Transform = player.handle.eyeLocation.transform()): State {
        val state = State(
            player.handle,
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
