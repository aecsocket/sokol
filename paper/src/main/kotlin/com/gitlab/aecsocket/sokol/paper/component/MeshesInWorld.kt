package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object MeshesInWorld : SimplePersistentComponent {
    override val componentType get() = MeshesInWorld::class
    override val key = SokolAPI.key("meshes_in_world")
    val Type = ComponentType.singletonComponent(key, this)
}

data class MeshesInWorldInstance(
    val meshEntries: List<MeshEntry>
) : SokolComponent {
    override val componentType get() = MeshesInWorldInstance::class
}

object MeshesInWorldInstanceTarget : SokolSystem

@All(MeshesInWorld::class, MeshProvider::class, PositionRead::class, PlayerTracked::class)
@None(MeshesInWorldInstance::class)
@Before(MeshesInWorldInstanceTarget::class)
@After(MeshProviderTarget::class, PositionAccessTarget::class)
class MeshesInWorldSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mMeshProvider = ids.mapper<MeshProvider>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mPlayerTracked = ids.mapper<PlayerTracked>()
    private val mMeshesInWorldInstance = ids.mapper<MeshesInWorldInstance>()

    data class Create(
        val sendToPlayers: Boolean
    ) : SokolEvent

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
         val meshes = mMeshProvider.get(entity)
        val positionRead = mPositionRead.get(entity)
        val playerTracked = mPlayerTracked.get(entity)

        val transform = positionRead.transform
        val meshEntries = meshes.create(transform, playerTracked.trackedPlayers)

        if (event.sendToPlayers) {
            val players = playerTracked.trackedPlayers()
            meshEntries.forEach { (mesh) ->
                mesh.spawn(players)
            }
        }

        mMeshesInWorldInstance.set(entity, MeshesInWorldInstance(meshEntries))
    }
}

@All(MeshesInWorldInstance::class, PositionRead::class, PlayerTracked::class)
@After(MeshesInWorldInstanceTarget::class, PositionAccessTarget::class, PlayerTrackedTarget::class)
class MeshesInWorldInstanceSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mMeshesInWorldInstance = ids.mapper<MeshesInWorldInstance>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mPlayerTracked = ids.mapper<PlayerTracked>()

    object Remove : SokolEvent

    data class Show(
        val player: Player
    ) : SokolEvent

    data class Hide(
        val player: Player
    ) : SokolEvent

    data class Glowing(
        val state: Boolean,
        val players: Iterable<Player>
    ) : SokolEvent

    data class GlowingColor(
        val color: NamedTextColor
    ) : SokolEvent

    @Subscribe
    fun on(event: Composite.Attach, entity: SokolEntity) {
        val meshesInWorldInstance = mMeshesInWorldInstance.get(entity)
        val playerTracked = mPlayerTracked.get(entity)

        meshesInWorldInstance.meshEntries.forEach { (mesh) ->
            mesh.updateTrackedPlayers { playerTracked.trackedPlayers() }
        }
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val meshesInWorldInstance = mMeshesInWorldInstance.get(entity)

        meshesInWorldInstance.meshEntries.forEach { (mesh) ->
            AlexandriaAPI.meshes.remove(mesh.id)
        }
        mMeshesInWorldInstance.remove(entity)
    }

    @Subscribe
    fun on(event: Show, entity: SokolEntity) {
        val meshesInWorldInstance = mMeshesInWorldInstance.get(entity)

        meshesInWorldInstance.meshEntries.forEach { (mesh) ->
            mesh.spawn(event.player)
        }
    }

    @Subscribe
    fun on(event: Hide, entity: SokolEntity) {
        val meshesInWorldInstance = mMeshesInWorldInstance.get(entity)

        meshesInWorldInstance.meshEntries.forEach { (mesh) ->
            mesh.remove(event.player)
        }
    }

    @Subscribe
    fun on(event: Glowing, entity: SokolEntity) {
        val meshesInWorldInstance = mMeshesInWorldInstance.get(entity)

        meshesInWorldInstance.meshEntries.forEach { (mesh) ->
            mesh.glowing(event.state, event.players)
        }
    }

    @Subscribe
    fun on(event: GlowingColor, entity: SokolEntity) {
        val meshesInWorldInstance = mMeshesInWorldInstance.get(entity)

        meshesInWorldInstance.meshEntries.forEach { (mesh) ->
            mesh.glowingColor = event.color
        }
    }

    @Subscribe
    fun on(event: ReloadEvent, entity: SokolEntity) {
        entity.callSingle(Remove)
        entity.callSingle(MeshesInWorldSystem.Create(true))
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val meshesInWorldInstance = mMeshesInWorldInstance.get(entity)
        val positionRead = mPositionRead.get(entity)

        val transform = positionRead.transform
        meshesInWorldInstance.meshEntries.forEach { (mesh, meshTransform) ->
            mesh.transform = transform * meshTransform
        }
    }
}

@All(MeshesInWorld::class, IsMob::class)
class MeshesInWorldMobSystem(ids: ComponentIdAccess) : SokolSystem {
    @Subscribe
    fun on(event: MobEvent.Spawn, entity: SokolEntity) {
        entity.callSingle(MeshesInWorldSystem.Create(false))
    }

    @Subscribe
    fun on(event: MobEvent.AddToWorld, entity: SokolEntity) {
        entity.callSingle(MeshesInWorldSystem.Create(false))
    }

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        entity.callSingle(MeshesInWorldInstanceSystem.Remove)
    }

    @Subscribe
    fun on(event: MobEvent.Show, entity: SokolEntity) {
        entity.callSingle(MeshesInWorldInstanceSystem.Show(event.player))
    }

    @Subscribe
    fun on(event: MobEvent.Hide, entity: SokolEntity) {
        entity.callSingle(MeshesInWorldInstanceSystem.Hide(event.player))
    }
}
