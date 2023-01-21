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

    fun init(ctx: Sokol.InitContext) {
        ctx.persistentComponent(Type)
        ctx.transientComponent<MeshesInWorldInstance>()
        ctx.system { MeshesInWorldInstanceTarget }
        ctx.system { MeshesInWorldSystem(it) }
        ctx.system { MeshesInWorldInstanceSystem(it) }
        ctx.system { MeshesInWorldMobSystem(ctx.sokol, it) }
    }
}

data class MeshesInWorldInstance(
    val meshEntries: List<MeshEntry>
) : SokolComponent {
    override val componentType get() = MeshesInWorldInstance::class
}

object MeshesInWorldInstanceTarget : SokolSystem

@All(MeshesInWorld::class, MeshProvider::class, PositionAccess::class, PlayerTracked::class)
@None(MeshesInWorldInstance::class)
@Before(MeshesInWorldInstanceTarget::class)
@After(MeshProviderTarget::class, PositionAccessTarget::class)
class MeshesInWorldSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mMeshProvider = ids.mapper<MeshProvider>()
    private val mPositionAccess = ids.mapper<PositionAccess>()
    private val mPlayerTracked = ids.mapper<PlayerTracked>()
    private val mMeshesInWorldInstance = ids.mapper<MeshesInWorldInstance>()

    data class Create(
        val sendToPlayers: Boolean
    ) : SokolEvent

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val meshes = mMeshProvider.get(entity)
        val positionRead = mPositionAccess.get(entity)
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

@All(MeshesInWorldInstance::class, PositionAccess::class, PlayerTracked::class)
@After(PlayerTrackedUpdateTarget::class)
class MeshesInWorldInstanceSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mMeshesInWorldInstance = ids.mapper<MeshesInWorldInstance>()
    private val mPositionAccess = ids.mapper<PositionAccess>()
    private val mPlayerTracked = ids.mapper<PlayerTracked>()

    object Update : SokolEvent

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
    fun on(event: Update, entity: SokolEntity) {
        val meshesInWorldInstance = mMeshesInWorldInstance.get(entity)
        val positionAccess = mPositionAccess.get(entity)

        val transform = positionAccess.transform
        meshesInWorldInstance.meshEntries.forEach { (mesh, meshTransform) ->
            mesh.transform = transform * meshTransform
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
}

@All(IsMob::class)
@After(MeshesInWorldInstanceTarget::class, PositionAccessTarget::class, PlayerTrackedTarget::class)
class MeshesInWorldMobSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mIsMob = ids.mapper<IsMob>()
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: MobEvent.Spawn, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldSystem.Create(false))
    }

    @Subscribe
    fun on(event: MobEvent.AddToWorld, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldSystem.Create(false))
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldInstanceSystem.Update)
    }

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldInstanceSystem.Remove)
    }

    @Subscribe
    fun on(event: MobEvent.Show, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldInstanceSystem.Show(event.player))
    }

    @Subscribe
    fun on(event: MobEvent.Hide, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldInstanceSystem.Hide(event.player))
    }

    @Subscribe
    fun on(event: ReloadEvent, entity: SokolEntity) {
        val mob = mIsMob.get(entity).mob
        val oldEntity = sokol.resolver.mobTrackedBy(mob) ?: return
        mComposite.forwardAll(oldEntity, MeshesInWorldInstanceSystem.Remove)
        mComposite.forwardAll(entity, MeshesInWorldSystem.Create(true))
    }
}
