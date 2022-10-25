package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.NodeKey
import java.util.UUID

private const val STATIC_MESHES = "static_meshes"
private const val BACKING = "backing"
private const val MESH_IDS = "mesh_ids"

data class StaticMeshes(
    val backing: Config,
    var meshIds: List<UUID> = emptyList(),
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("static_meshes")
    }

    override val componentType get() = StaticMeshes::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .set(BACKING) { makeString(backing.id) }
        .set(MESH_IDS) { makeList().apply { meshIds.forEach { add { makeUUID(it) } } } }

    override fun write(node: ConfigurationNode) {
        node.node(BACKING).set(backing.id)
        node.node(MESH_IDS).setList(UUID::class.java, meshIds)
    }

    @ConfigSerializable
    data class Config(
        @NodeKey override val id: String,
        val parts: List<Meshes.PartDefinition> = emptyList(),
        val transform: Transform = Transform.Identity,
        val interpolated: Boolean = true,
    ) : Keyed

    class Type : RegistryComponentType<Config>(Config::class, StaticMeshes::class, STATIC_MESHES) {
        override val key get() = Key

        override fun read(tag: NBTTag) = tag.asCompound().run { StaticMeshes(
            entry(get(BACKING) { asString() }),
            getList(MESH_IDS).map { it.asUUID() }
        ) }

        override fun read(node: ConfigurationNode) = StaticMeshes(
            entry(node.node(BACKING).force()),
            node.node(MESH_IDS).force<ArrayList<UUID>>(),
        )

        override fun readFactory(node: ConfigurationNode): PersistentComponentFactory {
            val backing = entry(node.force())
            return PersistentComponentFactory { StaticMeshes(backing) }
        }
    }
}

@All(Meshes::class, StaticMeshes::class)
class StaticMeshesInjectorSystem(engine: SokolEngine) : SokolSystem {
    private val mConsumer = engine.componentMapper<Meshes>()
    private val mProvider = engine.componentMapper<StaticMeshes>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntityAccess) {
        val consumer = mConsumer.map(entity)
        val provider = mProvider.map(entity)

        consumer.parts = provider.backing.parts.mapIndexed { idx, def ->
            Meshes.PartEntry(
                def,
                if (idx >= provider.meshIds.size) null
                else AlexandriaAPI.meshes[provider.meshIds[idx]]
            )
        }
        consumer.transform = provider.backing.transform
        consumer.interpolated = provider.backing.interpolated
    }

    @Subscribe
    fun on(event: MeshesSystem.CreateMesh, entity: SokolEntityAccess) {
        val consumer = mConsumer.map(entity)
        val provider = mProvider.map(entity)

        provider.meshIds = consumer.parts.mapNotNull { it.part?.id }
    }
}
