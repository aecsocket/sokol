package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.UUID

private const val MESH_IDS = "mesh_ids"

data class StaticMeshes(
    val profile: Profile,
    var meshIds: List<UUID> = emptyList(),
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("static_meshes")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = StaticMeshes::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .set(MESH_IDS) { makeList().apply { meshIds.forEach { add { makeUUID(it) } } } }

    override fun write(node: ConfigurationNode) {
        node.node(MESH_IDS).setList(UUID::class.java, meshIds)
    }

    @ConfigSerializable
    data class Profile(
        @Required val parts: List<Meshes.PartDefinition>,
        val transform: Transform = Transform.Identity,
        val interpolated: Boolean = true,
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = StaticMeshes(this,
            tag.asList().map { it.asUUID() })

        override fun read(node: ConfigurationNode) = StaticMeshes(this,
            node.force<ArrayList<UUID>>())
    }
}

@All(StaticMeshes::class)
class StaticMeshesSystem(engine: SokolEngine) : SokolSystem {
    private val mStaticMeshes = engine.componentMapper<StaticMeshes>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val staticMeshes = mStaticMeshes.map(entity)

        entity.components.set(Meshes(
            staticMeshes.profile.parts.mapIndexed { idx, def -> Meshes.PartEntry(
                def,
                if (idx >= staticMeshes.meshIds.size) null
                else AlexandriaAPI.meshes[staticMeshes.meshIds[idx]]
            ) },
            staticMeshes.profile.transform,
            staticMeshes.profile.interpolated
        ))
    }

    @Subscribe
    fun on(event: Meshes.CreateMesh, entity: SokolEntity) {
        val staticMeshes = mStaticMeshes.map(entity)

        staticMeshes.meshIds = event.newParts.mapNotNull { it.part?.id }
    }
}
