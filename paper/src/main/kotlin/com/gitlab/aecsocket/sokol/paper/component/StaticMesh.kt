package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeAlexandriaKey
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.TypeSerializer

private const val STATIC_MESHES = "static_meshes"
private const val PARTS = "parts"
private const val ITEM = "item"
private const val TRANSFORM = "transform"
private const val INTERPOLATED = "interpolated"

data class StaticMesh(val backing: Config) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("static_mesh")
    }

    override val componentType get() = StaticMesh::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeString(backing.id)

    override fun write(node: ConfigurationNode) {
        node.set(backing.id)
    }

    data class Config(
        override val id: String,
        val parts: List<Mesh.PartDefinition>,
        val transform: Transform,
        val interpolated: Boolean,
    ) : Keyed

    class Type : RegistryComponentType<Config>(Config::class, STATIC_MESHES) {
        override val key get() = Key

        override fun read(tag: NBTTag) = StaticMesh(
            entry(tag.asString())
        )

        override fun read(node: ConfigurationNode) = StaticMesh(
            entry(node.force())
        )

        override fun readFactory(node: ConfigurationNode): PersistentComponentFactory {
            val backing = entry(node.force())
            return PersistentComponentFactory { StaticMesh(backing) }
        }
    }

    object ConfigSerializer : TypeSerializer<Config> {
        override fun serialize(type: java.lang.reflect.Type, obj: Config?, node: ConfigurationNode) {}

        override fun deserialize(type: java.lang.reflect.Type, node: ConfigurationNode) = Config(
            parseNodeAlexandriaKey(type, node),
            node.node(PARTS).childrenList().map { child ->
                Mesh.PartDefinition(
                    child.node(ITEM).force<ItemDescriptor>().create().serializeAsBytes(),
                    child.node(TRANSFORM).get { Transform.Identity },
                )
            },
            node.node(TRANSFORM).get { Transform.Identity },
            node.node(INTERPOLATED).get { true },
        )
    }
}

@All(Mesh::class, StaticMesh::class)
@Priority(PRIORITY_EARLY)
class StaticMeshSystem(engine: SokolEngine) : SokolSystem {
    private val mMesh = engine.componentMapper<Mesh>()
    private val mStaticMesh = engine.componentMapper<StaticMesh>()

    private fun assign(entity: SokolEntityAccess) {
        val mesh = mMesh.map(entity)
        val staticMesh = mStaticMesh.map(entity).backing

        mesh.parts = staticMesh.parts.map { Mesh.PartEntry(it) }
        mesh.transform = staticMesh.transform
        mesh.interpolated = staticMesh.interpolated
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntityAccess) {
        assign(entity)
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntityAccess) {
        assign(entity)
    }
}
