package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.MeshSettings
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class MeshProviderFromItem(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("mesh_provider_from_item")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { MeshProviderFromItemSystem(ctx.sokol, it) }
            ctx.system { MeshProviderFromItemForwardSystem(it) }
        }
    }

    override val componentType get() = MeshProviderFromItem::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val transform: Transform = Transform.Identity,
        val meshSettings: MeshSettings = MeshSettings()
    ) : SimpleComponentProfile<MeshProviderFromItem> {
        override val componentType get() = MeshProviderFromItem::class

        override fun createEmpty() = ComponentBlueprint { MeshProviderFromItem(this) }
    }
}

@All(MeshProviderFromItem::class)
@None(MeshProvider::class)
class MeshProviderFromItemSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mMeshProviderFromItem = ids.mapper<MeshProviderFromItem>()
    private val mMeshProvider = ids.mapper<MeshProvider>()

    object Construct : SokolEvent

    @Subscribe
    fun on(event: Construct, entity: SokolEntity) {
        val meshProviderFromItem = mMeshProviderFromItem.get(entity).profile

        mMeshProvider.set(entity, MeshProvider { transform, playerTracker ->
            val itemEntity = sokol.persistence.blueprintOf(entity).create()
            val item = sokol.hoster.createItemForm(itemEntity)

            val transform = transform * meshProviderFromItem.transform
            val mesh = AlexandriaAPI.meshes.createItem(transform, playerTracker, meshProviderFromItem.meshSettings, item)

            listOf(MeshEntry(mesh, meshProviderFromItem.transform))
        })
    }
}

@Before(MeshProviderTarget::class)
class MeshProviderFromItemForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, MeshProviderFromItemSystem.Construct)
    }
}
