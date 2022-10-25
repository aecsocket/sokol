package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.Mesh
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.TypeSerializer

private const val ITEM = "item"
private const val TRANSFORM = "transform"

class Meshes : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes")
    }

    data class PartDefinition(
        val item: ItemStack,
        val transform: Transform = Transform.Identity,
    )

    data class PartEntry(
        val definition: PartDefinition,
        val part: Mesh? = null,
    )

    override val componentType get() = Meshes::class.java
    override val key get() = Key

    lateinit var parts: List<PartEntry>
    lateinit var transform: Transform
    var interpolated = true

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = Meshes()

        override fun read(node: ConfigurationNode) = Meshes()

        override fun readFactory(node: ConfigurationNode) = PersistentComponentFactory { Meshes() }
    }

    object PartDefinitionSerializer : TypeSerializer<PartDefinition> {
        override fun serialize(type: java.lang.reflect.Type, obj: PartDefinition?, node: ConfigurationNode) {}

        override fun deserialize(type: java.lang.reflect.Type, node: ConfigurationNode) = PartDefinition(
            node.node(ITEM).force<ItemDescriptor>().create(),
            node.node(TRANSFORM).get { Transform.Identity }
        )
    }
}

@All(Position::class, HostedByMob::class, Meshes::class)
class MeshesSystem(engine: SokolEngine) : SokolSystem {
    private val mPosition = engine.componentMapper<Position>()
    private val mMob = engine.componentMapper<HostedByMob>()
    private val mMeshes = engine.componentMapper<Meshes>()

    private fun createMesh(
        transform: Transform,
        mob: Entity,
        meshes: Meshes,
        entity: SokolEntityAccess,
        send: Boolean = false
    ) {
        val tracked = mob.trackedPlayers
        val parts = meshes.parts

        meshes.parts = parts.map { entry ->
            val (def, part) = entry
            // make a new mesh only if we don't have a valid one
            if (part == null) {
                val item = def.item
                val newPart = AlexandriaAPI.meshes.create(
                    item,
                    transform + def.transform,
                    { mob.trackedPlayers },
                    meshes.interpolated,
                )
                if (send) {
                    newPart.spawn(tracked)
                }
                Meshes.PartEntry(def, newPart)
            } else entry
        }

        entity.call(CreateMesh)
    }

    private data class PartContext(
        val part: Mesh,
        val definition: Meshes.PartDefinition
    )

    private fun forEachPart(meshes: Meshes, action: (PartContext) -> Unit) {
        meshes.parts.forEach { (def, part) ->
            part?.let {
                action(PartContext(it, def))
            }
        }
    }

    private fun removeMesh(meshes: Meshes) {
        meshes.parts.forEach { (_, part) ->
            part?.let {
                AlexandriaAPI.meshes.remove(it.id)
            }
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntityAccess) {
        val location = mPosition.map(entity)
        val mob = mMob.map(entity).mob
        val meshes = mMeshes.map(entity)

        createMesh(location.transform, mob, meshes, entity)
    }

    @Subscribe
    fun on(event: MobEvent.Show, entity: SokolEntityAccess) {
        val meshes = mMeshes.map(entity)

        forEachPart(meshes) { (part) ->
            part.spawn(event.player)
        }
    }

    @Subscribe
    fun on(event: MobEvent.Hide, entity: SokolEntityAccess) {
        val meshes = mMeshes.map(entity)

        forEachPart(meshes) { (part) ->
            part.remove(event.player)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntityAccess) {
        val meshes = mMeshes.map(entity)

        removeMesh(meshes)
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntityAccess) {
        val location = mPosition.map(entity)
        val mob = mMob.map(entity).mob
        val meshes = mMeshes.map(entity)

        removeMesh(meshes)
        createMesh(location.transform, mob, meshes, entity, true)
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntityAccess) {
        val location = mPosition.map(entity)
        val meshes = mMeshes.map(entity)

        forEachPart(meshes) { (part, def) ->
            part.transform = location.transform + def.transform
        }
    }

    object CreateMesh : SokolEvent
}
