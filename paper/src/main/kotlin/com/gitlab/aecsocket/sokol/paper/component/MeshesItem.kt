package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class MeshesItem(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes_item")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = MeshesItem::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val transform: Transform = Transform.Identity,
        val interpolated: Boolean = true
    ) : SimpleComponentProfile {
        override val componentType get() = MeshesItem::class

        override fun createEmpty() = ComponentBlueprint { MeshesItem(this) }
    }
}

@All(MeshesItem::class)
@Before(MeshesTarget::class)
class MeshesItemSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mMeshesItem = ids.mapper<MeshesItem>()
    private val mMeshes = ids.mapper<Meshes>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mMeshes.set(entity, Meshes)
    }

    @Subscribe
    fun on(event: Meshes.Create, entity: SokolEntity) {
        val meshesItem = mMeshesItem.get(entity).profile

        val itemEntity = sokol.persistence.blueprintOf(entity).create()
        val item = sokol.hoster.createItemForm(itemEntity)

        val transform = event.transform * meshesItem.transform
        val mesh = AlexandriaAPI.meshes.create(item, transform, event.getTrackedPlayers, meshesItem.interpolated)
        event.meshes.add(MeshEntry(mesh, meshesItem.transform))
    }
}
