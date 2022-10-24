package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asTransform
import com.gitlab.aecsocket.sokol.core.extension.makeTransform
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.TypeSerializer
import java.util.Base64
import java.util.UUID

private const val PARTS = "parts"
private const val TRANSFORM = "transform"
private const val ITEM_DATA = "item_data"
private const val DEFINITION = "definition"
private const val ID = "id"

data class Mesh(
    var parts: List<PartEntry> = emptyList(),
    var transform: Transform = Transform.Identity,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("mesh")
    }

    @ConfigSerializable
    data class PartEntry(
        val definition: PartDefinition,
        val id: UUID? = null,
    )

    override val componentType get() = Mesh::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .set(PARTS) { makeList().apply { parts.forEach { add { makeCompound()
            .set(DEFINITION) {makeCompound()
                .set(ITEM_DATA) { makeByteArray(it.definition.itemData) }
                .set(TRANSFORM) { makeTransform(it.definition.transform) }
            }
            .setOrClear(ID) { it.id?.let { makeUUID(it) } }
        } } } }
        .set(TRANSFORM) { makeTransform(transform) }

    override fun write(node: ConfigurationNode) {
        node.node(PARTS).setList(PartEntry::class.java, parts)
        node.node(TRANSFORM).set(transform)
    }

    class PartDefinition(
        val itemData: ByteArray,
        val transform: Transform = Transform.Identity,
    )

    object PartDefinitionSerializer : TypeSerializer<PartDefinition> {
        override fun serialize(type: java.lang.reflect.Type, obj: PartDefinition?, node: ConfigurationNode) {
            if (obj == null) node.set(null)
            else {
                node.node(ITEM_DATA).set(Base64.getEncoder().encodeToString(obj.itemData))
                node.node(TRANSFORM).set(obj.transform)
            }
        }

        override fun deserialize(type: java.lang.reflect.Type, node: ConfigurationNode) = PartDefinition(
            Base64.getDecoder().decode(node.node(ITEM_DATA).force<String>()),
            node.node(TRANSFORM).get { Transform.Identity },
        )
    }

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = tag.asCompound().run { Mesh(
            get(PARTS) { asList().map { it.asCompound().run { PartEntry(
                get(DEFINITION) { asCompound().run { PartDefinition(
                    get(ITEM_DATA) { asByteArray() },
                    get(TRANSFORM) { asTransform() },
                ) } },
                getOr(ID) { asUUID() }
            ) } } },
            get(TRANSFORM) { asTransform() },
        ) }

        override fun read(node: ConfigurationNode) = Mesh(
            node.node(PARTS).get { ArrayList() },
            node.node(TRANSFORM).get { Transform.Identity },
        )

        override fun readFactory(node: ConfigurationNode) = PersistentComponentFactory { Mesh() }
    }
}

@All(Position::class, HostedByMob::class, Mesh::class)
class MeshSystem(engine: SokolEngine) : SokolSystem {
    private val mPosition = engine.componentMapper<Position>()
    private val mMob = engine.componentMapper<HostedByMob>()
    private val mMesh = engine.componentMapper<Mesh>()

    private fun createMesh(transform: Transform, mob: Entity, mesh: Mesh, send: Boolean = false) {
        val tracked = mob.trackedPlayers
        val parts = mesh.parts
        mesh.parts = parts.map { entry ->
            val (part, id) = entry
            // make a new mesh only if we either
            //  · don't have one
            //  · or it's invalid (e.g. loaded after a server restart, so stale UUID here)
            if (id == null || !AlexandriaAPI.meshes.contains(id)) {
                val item = ItemStack.deserializeBytes(part.itemData)
                val inst = AlexandriaAPI.meshes.create(
                    item,
                    transform + part.transform,
                    { mob.trackedPlayers },
                    true /* todo */
                )
                if (send) {
                    inst.spawn(tracked)
                }
                Mesh.PartEntry(part, inst.id)
            } else entry
        }
    }

    private data class PartContext(
        val part: com.gitlab.aecsocket.alexandria.paper.Mesh,
        val definition: Mesh.PartDefinition
    )

    private fun forEachPart(mesh: Mesh, action: (PartContext) -> Unit) {
        mesh.parts.forEach { (definition, id) ->
            id?.let {
                AlexandriaAPI.meshes[id]?.let {
                    action(PartContext(it, definition))
                }
            }
        }
    }

    private fun removeMesh(mesh: Mesh) {
        mesh.parts.forEach { (_, id) ->
            if (id != null) {
                AlexandriaAPI.meshes.remove(id)
            }
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntityAccess) {
        // if we haven't defined the hosting entity as a mesh yet...
        // (if this entity was already in the world, and we just loaded it e.g. from chunk load...)
        // we can add it with the same logic as above
        val location = mPosition.map(entity)
        val mob = mMob.map(entity).mob
        val mesh = mMesh.map(entity)

        createMesh(location.transform, mob, mesh)
    }

    @Subscribe
    fun on(event: MobEvent.Show, entity: SokolEntityAccess) {
        val mesh = mMesh.map(entity)

        forEachPart(mesh) { (part) ->
            part.spawn(event.player)
        }
    }

    @Subscribe
    fun on(event: MobEvent.Hide, entity: SokolEntityAccess) {
        val mesh = mMesh.map(entity)

        forEachPart(mesh) { (part) ->
            part.remove(event.player)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntityAccess) {
        val mesh = mMesh.map(entity)

        removeMesh(mesh)
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntityAccess) {
        val location = mPosition.map(entity)
        val mob = mMob.map(entity).mob
        val mesh = mMesh.map(entity)

        removeMesh(mesh)
        createMesh(location.transform, mob, mesh, true)
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntityAccess) {
        val location = mPosition.map(entity)
        val mesh = mMesh.map(entity)

        forEachPart(mesh) { (part, definition) ->
            part.transform = location.transform + definition.transform
        }
    }
}
