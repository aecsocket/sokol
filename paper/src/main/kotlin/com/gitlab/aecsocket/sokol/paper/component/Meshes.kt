package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.Mesh
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.TypeSerializer

private const val ITEM = "item"
private const val TRANSFORM = "transform"

data class Meshes(
    val parts: List<PartEntry>,
    val transform: Transform,
    val interpolated: Boolean
) : SokolComponent {
    data class PartDefinition(
        val item: ItemStack,
        val transform: Transform = Transform.Identity,
    )

    data class PartEntry(
        val definition: PartDefinition,
        val part: Mesh? = null,
    )

    override val componentType get() = Meshes::class

    object PartDefinitionSerializer : TypeSerializer<PartDefinition> {
        override fun serialize(type: java.lang.reflect.Type, obj: PartDefinition?, node: ConfigurationNode) {}

        override fun deserialize(type: java.lang.reflect.Type, node: ConfigurationNode) = PartDefinition(
            node.node(ITEM).force<ItemDescriptor>().create(),
            node.node(TRANSFORM).get { Transform.Identity }
        )
    }
}

@All(Meshes::class, PositionRead::class, TrackedPlayersSupplier::class)
class MeshesSystem(engine: SokolEngine) : SokolSystem {
    private val mMeshes = engine.componentMapper<Meshes>()
    private val mPosition = engine.componentMapper<PositionRead>()
    private val mTrackedPlayersSupplier = engine.componentMapper<TrackedPlayersSupplier>()
    private val mComposite = engine.componentMapper<Composite>()

    private fun forEachPart(meshes: Meshes, action: (Pair<Mesh, Meshes.PartDefinition>) -> Unit) {
        meshes.parts.forEach { (def, part) ->
            part?.let {
                action(it to def)
            }
        }
    }

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val meshes = mMeshes.map(entity)
        val position = mPosition.map(entity)
        val trackedPlayers = mTrackedPlayersSupplier.map(entity).trackedPlayers

        val transform = position.transform
        val parts = meshes.parts

        val newParts = parts.map { entry ->
            val (def, part) = entry
            // make a new mesh only if we don't have a valid one
            if (part == null) {
                val item = def.item
                val newPart = AlexandriaAPI.meshes.create(
                    item,
                    transform + def.transform,
                    trackedPlayers,
                    meshes.interpolated,
                )
                if (event.sendToPlayers) {
                    newPart.spawn(trackedPlayers())
                }
                Meshes.PartEntry(def, newPart)
            } else entry
        }

        entity.call(Created(newParts))
        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Show, entity: SokolEntity) {
        val meshes = mMeshes.map(entity)

        forEachPart(meshes) { (part) ->
            part.spawn(event.player)
        }

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Hide, entity: SokolEntity) {
        val meshes = mMeshes.map(entity)

        forEachPart(meshes) { (part) ->
            part.remove(event.player)
        }

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val meshes = mMeshes.map(entity)

        forEachPart(meshes) { (part) ->
            AlexandriaAPI.meshes.remove(part.id)
        }

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: Reload, entity: SokolEntity) {
        entity.call(Remove)
        entity.call(Create(true))
    }

    @Subscribe
    fun on(event: Update, entity: SokolEntity) {
        val position = mPosition.map(entity)
        val meshes = mMeshes.map(entity)

        forEachPart(meshes) { (part, def) ->
            part.transform = position.transform + def.transform
        }

        mComposite.forward(entity, event)
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        entity.call(Create(false))
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
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        entity.call(Remove)
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        entity.call(Update)
    }

    data class Create(
        val sendToPlayers: Boolean,
    ) : SokolEvent

    data class Created(
        val newParts: List<Meshes.PartEntry>
    ) : SokolEvent

    data class Show(
        val player: Player
    ) : SokolEvent

    data class Hide(
        val player: Player
    ) : SokolEvent

    object Remove : SokolEvent

    object Reload : SokolEvent

    object Update : SokolEvent
}
