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
    val parts: List<PartDefinition>,
    val interpolated: Boolean
) : SokolComponent {
    override val componentType get() = Meshes::class

    data class PartDefinition(
        val item: ItemStack,
        val transform: Transform = Transform.Identity,
    )

    object PartDefinitionSerializer : TypeSerializer<PartDefinition> {
        override fun serialize(type: java.lang.reflect.Type, obj: PartDefinition?, node: ConfigurationNode) {}

        override fun deserialize(type: java.lang.reflect.Type, node: ConfigurationNode) = PartDefinition(
            node.node(ITEM).force<ItemDescriptor>().create(),
            node.node(TRANSFORM).get { Transform.Identity }
        )
    }
}

@All(Meshes::class, LocalTransform::class)
@After(LocalTransformTarget::class)
class MeshesSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mMeshes = mappers.componentMapper<Meshes>()
    private val mLocalTransform = mappers.componentMapper<LocalTransform>()

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val meshes = mMeshes.get(entity)
        val localTransform = mLocalTransform.get(entity)

        val transform = localTransform.transform
        val parts = meshes.parts.map { definition ->
            PartEntry(
                AlexandriaAPI.meshes.create(
                    definition.item,
                    event.transform + transform + definition.transform,
                    event.getTrackedPlayers,
                    meshes.interpolated
                ),
                transform,
                definition,
            )
        }

        event.parts.addAll(parts)
    }

    data class PartEntry(
        val mesh: Mesh,
        val transform: Transform,
        val definition: Meshes.PartDefinition,
    )

    data class Create(
        val parts: MutableList<PartEntry>,
        val transform: Transform,
        val getTrackedPlayers: () -> Iterable<Player>,
    ) : SokolEvent
}
