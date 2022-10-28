package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class MeshesItem(
    val profile: Profile,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes_item")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = MeshesItem::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val transform: Transform = Transform.Identity,
        val interpolated: Boolean = true,
    ) : NonReadingComponentProfile {
        override fun readEmpty() = MeshesItem(this)
    }
}

@All(MeshesItem::class, HostableByItem::class)
@Before(MeshesSystem::class)
class MeshesItemSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mMeshesItem = mappers.componentMapper<MeshesItem>()
    private val mMeshes = mappers.componentMapper<Meshes>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val itemMeshes = mMeshesItem.get(entity)

        val blueprint = entity.toBlueprint()
        mMeshesItem.remove(blueprint) // so that when building the entity, we don't recursively listen to Populate
        val item = sokol.entityHoster.createItemForm(blueprint)

        mMeshes.set(entity, Meshes(
            listOf(Meshes.PartDefinition(item)),
            itemMeshes.profile.interpolated
        ))
    }
}
