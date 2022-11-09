package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class MeshesStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes_static")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = MeshesStatic::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val parts: List<Meshes.PartDefinition>,
        val transform: Transform = Transform.Identity,
        val interpolated: Boolean = true,
    ) : SimpleComponentProfile {
        override fun readEmpty() = MeshesStatic(this)
    }
}

@All(MeshesStatic::class)
@Before(MeshesSystem::class)
class MeshesStaticSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mMeshesStatic = mappers.componentMapper<MeshesStatic>()
    private val mMeshes = mappers.componentMapper<Meshes>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val staticMeshes = mMeshesStatic.get(entity)

        mMeshes.set(entity, Meshes(
            staticMeshes.profile.parts,
            staticMeshes.profile.interpolated
        ))
    }
}
