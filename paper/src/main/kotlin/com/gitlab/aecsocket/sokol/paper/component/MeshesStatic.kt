package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val ITEM = "item"
private const val TRANSFORM = "transform"

data class MeshesStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes_static")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = MeshesStatic::class
    override val key get() = Key

    data class MeshDefinition(
        val item: ItemStack,
        val transform: Transform
    )

    object MeshDefinitionSerializer : TypeSerializer<MeshDefinition> {
        override fun serialize(type: Type, obj: MeshDefinition?, node: ConfigurationNode) {}

        override fun deserialize(type: Type, node: ConfigurationNode) = MeshDefinition(
            node.node(ITEM).force<ItemDescriptor>().create(),
            node.node(TRANSFORM).get { Transform.Identity }
        )
    }

    @ConfigSerializable
    data class Profile(
        @Required val parts: List<MeshDefinition>,
        val interpolated: Boolean = true
    ) : SimpleComponentProfile {
        override val componentType get() = MeshesStatic::class

        override fun createEmpty() = ComponentBlueprint { MeshesStatic(this) }
    }
}

@All(MeshesStatic::class)
@None(Meshes::class)
@Before(MeshesTarget::class)
class MeshesStaticSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mMeshesStatic = ids.mapper<MeshesStatic>()
    private val mMeshes = ids.mapper<Meshes>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mMeshes.set(entity,
            Meshes { transform, trackedPlayers ->
                val meshesStatic = mMeshesStatic.get(entity).profile

                meshesStatic.parts.map { (item, partTransform) ->
                    val mesh = AlexandriaAPI.meshes.create(
                        item,
                        transform * partTransform,
                        trackedPlayers,
                        meshesStatic.interpolated
                    )
                    MeshEntry(mesh, partTransform)
                }
            })
    }
}
