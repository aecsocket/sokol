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
    private val mMeshes = mappers.componentMapper<Meshes>()
    private val mPosition = mappers.componentMapper<PositionRead>()
    private val mSupplierTrackedPlayers = mappers.componentMapper<SupplierTrackedPlayers>()

    private fun forEachMesh(meshesInWorld: MeshesInWorld, action: (Mesh, Transform) -> Unit) {
        meshesInWorld.meshes.forEach { (id, transform) ->
            AlexandriaAPI.meshes[id]?.let { action(it, transform) }
        }
    }

    private fun create(entity: SokolEntity, sendToPlayers: Boolean = false) {
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

        if (sendToPlayers) {
            parts.forEach { (mesh) ->
                mesh.spawn(trackedPlayers)
            }
        }

        meshesInWorld.meshes = parts.map { MeshesInWorld.MeshEntry(it.mesh.id, it.transform) }
    }

    private fun remove(entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { mesh, _ ->
            AlexandriaAPI.meshes.remove(mesh.id)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        create(entity, false)
    }

    @Subscribe
    fun on(event: MobEvent.Show, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { mesh, _ ->
            mesh.spawn(event.player)
        }
    }

    @Subscribe
    fun on(event: MobEvent.Hide, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { mesh, _ ->
            mesh.remove(event.player)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        remove(entity)
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val position = mPosition.get(entity)
        val meshesInWorld = mMeshesInWorld.get(entity)
        val meshes = mMeshes.get(entity)

        val rootTransform = position.transform
        forEachMesh(meshesInWorld) { mesh, transform ->
            mesh.transform = rootTransform + transform
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntity) {
        remove(entity)
        create(entity, true)
    }
}
