package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.Mesh
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asTransform
import com.gitlab.aecsocket.sokol.core.extension.makeTransform
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.colliderCompositeHitPath
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.UUID

private const val ID = "id"
private const val TRANSFORM = "transform"

data class MeshesInWorld(
    var meshes: List<MeshEntry>,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes_in_world")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = MeshesInWorld::class
    override val key get() = Key

    @ConfigSerializable
    data class MeshEntry(
        val id: UUID,
        val transform: Transform,
    )

    override fun write(ctx: NBTTagContext) = ctx.makeList().apply { meshes.forEach { add { makeCompound()
        .set(ID) { makeUUID(it.id) }
        .set(TRANSFORM) { makeTransform(it.transform) }
    } } }

    override fun write(node: ConfigurationNode) {
        node.setList(MeshEntry::class.java, meshes)
    }

    object Profile : ComponentProfile {
        override fun read(tag: NBTTag) = MeshesInWorld(tag.asList().map { it.asCompound().run { MeshEntry(
            get(ID) { asUUID() },
            get(TRANSFORM) { asTransform() }
        ) } })

        override fun read(node: ConfigurationNode) = MeshesInWorld(node.force<ArrayList<MeshEntry>>())

        override fun readEmpty() = MeshesInWorld(emptyList())
    }
}

@All(MeshesInWorld::class, Meshes::class, PositionRead::class, SupplierTrackedPlayers::class)
@After(MeshesSystem::class, PositionSystem::class, SupplierTrackedPlayersTarget::class)
class MeshesInWorldSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mMeshesInWorld = mappers.componentMapper<MeshesInWorld>()
    private val mPosition = mappers.componentMapper<PositionRead>()
    private val mSupplierTrackedPlayers = mappers.componentMapper<SupplierTrackedPlayers>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mComposite = mappers.componentMapper<Composite>()

    private fun forEachMesh(meshesInWorld: MeshesInWorld, action: (Mesh, Transform) -> Unit) {
        meshesInWorld.meshes.forEach { (id, transform) ->
            AlexandriaAPI.meshes[id]?.let { action(it, transform) }
        }
    }

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)
        val position = mPosition.get(entity)
        val supplierTrackedPlayers = mSupplierTrackedPlayers.get(entity)

        val getTrackedPlayers = supplierTrackedPlayers.trackedPlayers
        val trackedPlayers = getTrackedPlayers()

        val transform = position.transform
        val (parts) = entity.call(MeshesSystem.Create(
            ArrayList(),
            transform,
            getTrackedPlayers,
        ))

        if (event.sendToPlayers) {
            parts.forEach { (mesh) ->
                mesh.spawn(trackedPlayers)
            }
        }

        meshesInWorld.meshes = parts.map { MeshesInWorld.MeshEntry(it.mesh.id, it.transform) }

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { mesh, _ ->
            AlexandriaAPI.meshes.remove(mesh.id)
        }
        meshesInWorld.meshes = emptyList()

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Show, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { mesh, _ ->
            mesh.spawn(event.player)
        }

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Hide, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { mesh, _ ->
            mesh.remove(event.player)
        }

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Update, entity: SokolEntity) {
        val position = mPosition.get(entity)
        val meshesInWorld = mMeshesInWorld.get(entity)

        val entityTransform = position.transform
        forEachMesh(meshesInWorld) { mesh, transform ->
            mesh.transform = entityTransform + transform
        }

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Glow, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { mesh, _ ->
            mesh.glowing(event.state, event.players)
        }
    }

    @Subscribe
    fun on(event: GlowColor, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { mesh, _ ->
            mesh.glowingColor = event.color
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        entity.call(Create(false))
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        entity.call(Remove)
    }

    @Subscribe
    fun on(event: MobEvent.Show, entity: SokolEntity) {
        entity.call(Show(event.player))
    }

    @Subscribe
    fun on(event: MobEvent.Hide, entity: SokolEntity) {
        entity.call(Hide(event.player))
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        entity.call(Update)
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntity) {
        entity.call(Remove)
        entity.call(Create(true))
    }

    /*private fun glow(entity: SokolEntity, state: Boolean, player: Player, childIdx: Int) {
        val hitPath = colliderCompositeHitPath(mCollider.getOr(entity), childIdx)
        mComposite.child(entity, hitPath)?.call(Glow(state, setOf(player)))
    }

    @Subscribe
    fun on(event: EntityHover.StartHovered, entity: SokolEntity) {
        glow(entity, true, event.player, event.newTestResult.triangleIndex())
    }

    @Subscribe
    fun on(event: EntityHover.ChangeHoverIndex, entity: SokolEntity) {
        glow(entity, false, event.player, event.oldIndex)
        glow(entity, true, event.player, event.newIndex)
    }

    @Subscribe
    fun on(event: EntityHover.StopHovered, entity: SokolEntity) {
        glow(entity, false, event.player, event.oldTestResult.triangleIndex())
    }*/

    data class Create(
        val sendToPlayers: Boolean
    ) : SokolEvent

    object Remove : SokolEvent

    data class Show(
        val player: Player
    ) : SokolEvent

    data class Hide(
        val player: Player
    ) : SokolEvent

    object Update : SokolEvent

    data class Glow(
        val state: Boolean,
        val players: Iterable<Player>
    ) : SokolEvent

    data class GlowColor(
        val color: NamedTextColor
    ) : SokolEvent
}
