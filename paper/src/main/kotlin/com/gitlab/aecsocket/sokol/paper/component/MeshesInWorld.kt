package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.Mesh
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asTransform
import com.gitlab.aecsocket.sokol.core.extension.makeTransform
import com.gitlab.aecsocket.sokol.paper.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.UUID

private const val MESHES = "meshes"
private const val ID = "id"
private const val TRANSFORM = "transform"

data class MeshesInWorld(
    private val dMeshEntries: Delta<List<MeshEntry>>,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes_in_world")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = MeshesInWorld::class
    override val key get() = Key

    override val dirty get() = dMeshEntries.dirty

    constructor(
        meshEntries: List<MeshEntry>
    ) : this(Delta(meshEntries))

    var meshEntries by dMeshEntries

    lateinit var meshes: List<MeshEntryInstance>

    @ConfigSerializable
    data class MeshEntry(
        @Required val id: UUID,
        val transform: Transform = Transform.Identity,
    )

    data class MeshEntryInstance(
        val mesh: Mesh,
        val transform: Transform,
    )

    private fun NBTTagContext.makeMesh(mesh: MeshEntry) = makeCompound()
        .set(ID) { makeUUID(mesh.id) }
        .set(TRANSFORM) { makeTransform(mesh.transform) }

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .setList(MESHES, meshEntries) { mesh -> makeMesh(mesh) }

    override fun writeDelta(tag: NBTTag): NBTTag {
        val compound = tag.asCompound()
        dMeshEntries.ifDirty { compound.setList(MESHES, it) { mesh -> makeMesh(mesh) } }
        return tag
    }

    override fun write(node: ConfigurationNode) {
        node.node(MESHES).setList(MeshEntry::class.java, meshEntries)
    }

    object Profile : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound { compound -> MeshesInWorld(
            compound.getList(MESHES) { asCompound { mesh -> MeshEntry(
                mesh.get(ID) { asUUID() },
                mesh.get(TRANSFORM) { asTransform() }
            ) } }
        ) }

        override fun read(node: ConfigurationNode) = MeshesInWorld(
            node.node(MESHES).get { ArrayList() }
        )

        override fun readEmpty() = MeshesInWorld(emptyList())
    }
}

// only run this if we have an actual presence in the world
@All(PositionRead::class)
@After(MeshesInWorldSystem::class)
class MeshesInWorldForwardSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: CompositeSystem.AttachTo, entity: SokolEntity) {
        entity.call(MeshesInWorldSystem.Populate)
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(MeshesInWorldSystem.Populate)
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldSystem.Create(false))
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldSystem.Remove)
    }

    @Subscribe
    fun on(event: MobEvent.Show, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldSystem.Show(event.player))
    }

    @Subscribe
    fun on(event: MobEvent.Hide, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldSystem.Hide(event.player))
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldSystem.Update)
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshesInWorldSystem.Remove)
        mComposite.forwardAll(entity, MeshesInWorldSystem.Create(true))
    }
}

@All(MeshesInWorld::class, PositionRead::class, SupplierTrackedPlayers::class)
@After(PositionTarget::class, SupplierTrackedPlayersTarget::class)
class MeshesInWorldSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mMeshesInWorld = mappers.componentMapper<MeshesInWorld>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mSupplierTrackedPlayers = mappers.componentMapper<SupplierTrackedPlayers>()

    private fun forEachMesh(meshesInWorld: MeshesInWorld, action: (MeshesInWorld.MeshEntryInstance) -> Unit) {
        meshesInWorld.meshes.forEach(action)
    }

    @Subscribe
    fun on(event: Populate, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        meshesInWorld.meshes = meshesInWorld.meshEntries.mapNotNull {
            AlexandriaAPI.meshes[it.id]?.let { mesh ->
                MeshesInWorld.MeshEntryInstance(mesh, it.transform)
            }
        }
    }

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)
        val positionRead = mPositionRead.get(entity)
        val supplierTrackedPlayers = mSupplierTrackedPlayers.get(entity)
        if (meshesInWorld.meshes.isNotEmpty()) return

        val getTrackedPlayers = supplierTrackedPlayers.trackedPlayers
        val trackedPlayers = getTrackedPlayers()

        val transform = positionRead.transform
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

        meshesInWorld.meshEntries = parts.map { MeshesInWorld.MeshEntry(it.mesh.id, it.transform) }
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { (mesh) ->
            AlexandriaAPI.meshes.remove(mesh.id)
        }
        meshesInWorld.meshEntries = emptyList()
    }

    @Subscribe
    fun on(event: Show, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { (mesh) ->
            mesh.spawn(event.player)
        }
    }

    @Subscribe
    fun on(event: Hide, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { (mesh) ->
            mesh.remove(event.player)
        }
    }

    @Subscribe
    fun on(event: Update, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)
        val positionRead = mPositionRead.get(entity)

        val transform = positionRead.transform
        forEachMesh(meshesInWorld) { (mesh, meshTransform) ->
            mesh.transform = transform + meshTransform
        }
    }

    @Subscribe
    fun on(event: Glowing, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { (mesh) ->
            mesh.glowing(event.state, event.players)
        }
    }

    @Subscribe
    fun on(event: GlowingColor, entity: SokolEntity) {
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { (mesh) ->
            mesh.glowingColor = event.color
        }
    }

    @Subscribe
    fun on(event: CompositeSystem.AttachTo, entity: SokolEntity) {
        if (!event.fresh) return
        val supplierTrackedPlayers = mSupplierTrackedPlayers.getOr(event.parent)?.trackedPlayers ?: return
        val meshesInWorld = mMeshesInWorld.get(entity)

        forEachMesh(meshesInWorld) { (mesh) ->
            mesh.updateTrackedPlayers(supplierTrackedPlayers)
        }
    }

    object Populate : SokolEvent

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

    data class Glowing(
        val state: Boolean,
        val players: Iterable<Player>
    ) : SokolEvent

    data class GlowingColor(
        val color: NamedTextColor
    ) : SokolEvent
}
