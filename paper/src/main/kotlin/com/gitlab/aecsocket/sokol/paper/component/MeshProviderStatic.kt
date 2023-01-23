package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.MeshSettings
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
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

data class MeshProviderStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("mesh_provider_static")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { MeshProviderStaticSystem(it) }
            ctx.system { MeshProviderStaticForwardSystem(it) }
        }
    }

    override val componentType get() = MeshProviderStatic::class
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
        val meshSettings: MeshSettings = MeshSettings()
    ) : SimpleComponentProfile<MeshProviderStatic> {
        override val componentType get() = MeshProviderStatic::class

        override fun createEmpty() = ComponentBlueprint { MeshProviderStatic(this) }
    }
}

@All(MeshProviderStatic::class)
@None(MeshProvider::class)
class MeshProviderStaticSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mMeshProviderStatic = ids.mapper<MeshProviderStatic>()
    private val mMeshProvider = ids.mapper<MeshProvider>()

    object Construct : SokolEvent

    @Subscribe
    fun on(event: Construct, entity: SokolEntity) {
        val meshProviderStatic = mMeshProviderStatic.get(entity).profile

        mMeshProvider.set(entity, MeshProvider { transform, playerTracker ->
            meshProviderStatic.parts.map { (item, partTransform) ->
                val mesh = AlexandriaAPI.meshes.createItem(
                    transform * partTransform,
                    playerTracker,
                    meshProviderStatic.meshSettings,
                    item
                )
                MeshEntry(mesh, partTransform)
            }
        })
    }
}

@Before(MeshProviderTarget::class)
class MeshProviderStaticForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshProviderStaticSystem.Construct)
    }
}
