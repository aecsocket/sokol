package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asTransform
import com.gitlab.aecsocket.sokol.core.extension.makeTransform
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import kotlin.collections.ArrayList

private const val ID = "id"
private const val TRANSFORM = "transform"

data class MeshesInWorld(
    val dMeshes: Delta<List<MeshEntry>>
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes_in_world")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = MeshesInWorld::class
    override val key get() = Key

    override val dirty get() = dMeshes.dirty
    var meshes by dMeshes

    constructor(
        meshes: List<MeshEntry>
    ) : this(Delta(meshes))

    override fun write(ctx: NBTTagContext) = ctx.makeList().from(meshes) { mesh -> makeCompound()
        .set(ID) { makeUUID(mesh.mesh.id) }
        .set(TRANSFORM) { makeTransform(mesh.transform) }
    }

    override fun writeDelta(tag: NBTTag): NBTTag {
        return dMeshes.ifDirty { write(tag) } ?: tag
    }

    override fun serialize(node: ConfigurationNode) {
        meshes.forEach { mesh ->
            val nMesh = node.appendListNode()
            nMesh.node(ID).set(mesh.mesh.id)
            nMesh.node(TRANSFORM).set(mesh.transform)
        }
    }

    object Profile : ComponentProfile {
        override val componentType get() = MeshesInWorld::class

        override fun read(tag: NBTTag): ComponentBlueprint<MeshesInWorld> {
            val meshes = tag.asList().mapNotNull { it.asCompound { mesh ->
                AlexandriaAPI.meshes[mesh.get(ID) { asUUID() }]?.let { inst -> MeshEntry(
                    inst,
                    mesh.get(TRANSFORM) { asTransform() }
                ) }
            } }

            return ComponentBlueprint { MeshesInWorld(meshes) }
        }

        override fun deserialize(node: ConfigurationNode): ComponentBlueprint<MeshesInWorld> {
            val meshes = node.childrenList().mapNotNull { mesh ->
                AlexandriaAPI.meshes[mesh.node(ID).force()]?.let { inst -> MeshEntry(
                    inst,
                    mesh.node(TRANSFORM).get { Transform.Identity }
                ) }
            }

            return ComponentBlueprint { MeshesInWorld(meshes) }
        }

        override fun createEmpty() = ComponentBlueprint { MeshesInWorld(emptyList()) }
    }
}

@All(MeshesInWorld::class, Meshes::class, PositionRead::class, PlayerTracked::class)
@After(MeshesTarget::class)
class MeshesInWorldSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mMeshesInWorld = ids.mapper<MeshesInWorld>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mPlayerTracked = ids.mapper<PlayerTracked>()

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

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)
        val positionRead = mPositionRead.get(entity)
        val playerTracked = mPlayerTracked.get(entity)

        val transform = positionRead.transform
        val (meshes) = entity.callSingle(Meshes.Create(
            ArrayList(),
            transform
        ) { playerTracked.trackedPlayers() })

        if (event.sendToPlayers) {
            val players = playerTracked.trackedPlayers()
            meshes.forEach { (mesh) ->
                mesh.spawn(players)
            }
        }

        meshesInWorld.meshes = meshes
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        meshesInWorld.meshes.forEach { (mesh) ->
            AlexandriaAPI.meshes.remove(mesh.id)
        }
        meshesInWorld.meshes = emptyList()
    }

    @Subscribe
    fun on(event: Show, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        meshesInWorld.meshes.forEach { (mesh) ->
            mesh.spawn(event.player)
        }
    }

    @Subscribe
    fun on(event: Hide, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        meshesInWorld.meshes.forEach { (mesh) ->
            mesh.remove(event.player)
        }
    }

    @Subscribe
    fun on(event: ReloadEvent, entity: SokolEntity) {
        entity.callSingle(Remove)
        entity.callSingle(Create(true))
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)
        val positionRead = mPositionRead.get(entity)

        val parentTransform = positionRead.transform
        meshesInWorld.meshes.forEach { (mesh, transform) ->
            mesh.transform = parentTransform + transform
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
        entity.callSingle(MeshesInWorldSystem.Remove)
    }

    @Subscribe
    fun on(event: MobEvent.Show, entity: SokolEntity) {
        entity.callSingle(MeshesInWorldSystem.Show(event.player))
    }

    @Subscribe
    fun on(event: MobEvent.Hide, entity: SokolEntity) {
        entity.callSingle(MeshesInWorldSystem.Hide(event.player))
    }
}
