package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asTransform
import com.gitlab.aecsocket.sokol.core.extension.ofTransform
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

private const val LOCAL_TRANSFORM = "local_transform"

data class Mesh(
    val localTransform: Transform,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("mesh")
    }

    override val componentType get() = Mesh::class.java
    override val key get() = Key

    override fun write(): NBTWriter = { ofCompound()
        .set(LOCAL_TRANSFORM) { ofTransform(localTransform) }
    }

    override fun write(node: ConfigurationNode) {
        node.node(LOCAL_TRANSFORM).set(localTransform)
    }

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = tag.asCompound().run { Mesh(
            getOr(LOCAL_TRANSFORM) { asTransform() } ?: Transform.Identity
        ) }

        override fun read(node: ConfigurationNode) = Mesh(
            node.node(LOCAL_TRANSFORM).get { Transform.Identity }
        )
    }
}

@All(Location::class, HostedByEntity::class, Mesh::class)
class MeshSystem(engine: SokolEngine) : SokolSystem {
    private val mLocation = engine.componentMapper<Location>()
    private val mEntity = engine.componentMapper<HostedByEntity>()
    private val mMesh = engine.componentMapper<Mesh>()

    private fun createMesh(transform: Transform, mob: Entity, mesh: Mesh) {
        val instance = AlexandriaAPI.meshes.create(mob, transform + mesh.localTransform)
        instance.addPart(instance.Part(bukkitNextEntityId, Transform.Identity, ItemStack(Material.IRON_NUGGET).withMeta { meta ->
            meta.setCustomModelData(1)
        }), false)
    }

    @Subscribe
    fun on(event: EntityEvent.Host, space: SokolEngine.Space, entity: Int) {
        // define the mesh in Alexandria during host
        // because if we do this on entity add to world we *will* run into issues
        // with race condition between entity add and packet send
        val location = mLocation.map(space, entity)
        val mob = mEntity.map(space, entity).entity
        val mesh = mMesh.map(space, entity)

        createMesh(location.transform, mob, mesh)
    }

    @Subscribe
    fun on(event: SokolEvent.Add, space: SokolEngine.Space, entity: Int) {
        // if we haven't defined the hosting entity as a mesh yet...
        // (if this entity was already in the world, and we just loaded it e.g. from chunk load...)
        // we can add it with the same logic as above
        val location = mLocation.map(space, entity)
        val mob = mEntity.map(space, entity).entity
        val mesh = mMesh.map(space, entity)

        if (!AlexandriaAPI.meshes.contains(mob)) {
            createMesh(location.transform, mob, mesh)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Update, space: SokolEngine.Space, entity: Int) {
        val location = mLocation.map(space, entity)
        val mob = mEntity.map(space, entity).entity
        val mesh = mMesh.map(space, entity)

        AlexandriaAPI.meshes[mob]?.let { meshInstance ->
            meshInstance.meshTransform = location.transform + mesh.localTransform
        }
    }
}
