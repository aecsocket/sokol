package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode

class Mesh : PersistentComponent {
    override val type get() = Mesh
    override val key get() = Key

    override fun write(tag: CompoundNBTTag.Mutable) {}

    override fun write(node: ConfigurationNode) {}

    class Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: CompoundNBTTag) = Mesh()

        override fun read(node: ConfigurationNode) = Mesh()
    }

    companion object : ComponentType<Mesh> {
        val Key = SokolAPI.key("mesh")
    }
}

class MeshSystem(engine: SokolEngine) : SokolSystem {
    private val entFilter = engine.entityFilter(
        setOf(HostedByEntity, Mesh)
    )
    private val mMob = engine.componentMapper(HostedByEntity)
    private val mMesh = engine.componentMapper(Mesh)
    private val mCollider = engine.componentMapper(Collider)

    private fun createMesh(mob: Entity, mesh: Mesh, collider: Collider?) {
        val instance = AlexandriaAPI.meshes.create(mob, Transform(
            mob.location.position(),
            collider?.body?.rotation ?: Quaternion.Identity,
        ))
        instance.addPart(instance.Part(bukkitNextEntityId, Transform.Identity, ItemStack(Material.IRON_NUGGET).withMeta { meta ->
            meta.setCustomModelData(1)
        }), false)
    }

    override fun handle(space: SokolEngine.Space, event: SokolEvent) = when (event) {
        is HostByEntityEvent -> {
            space.entitiesBy(entFilter).forEach { entity ->
                val mob = mMob.map(space, entity).entity
                val mesh = mMesh.map(space, entity)
                val collider = mCollider.mapOr(space, entity)

                createMesh(mob, mesh, collider)
            }
        }
        is ByEntityEvent.Added -> {
            space.entitiesBy(entFilter).forEach { entity ->
                val mob = mMob.map(space, entity).entity
                val mesh = mMesh.map(space, entity)
                val collider = mCollider.mapOr(space, entity)

                if (!AlexandriaAPI.meshes.contains(mob)) {
                    createMesh(mob, mesh, collider)
                }
            }
        }
        is UpdateEvent -> {
            space.entitiesBy(entFilter).forEach { entity ->
                val mob = mMob.map(space, entity).entity
                val collider = mCollider.map(space, entity)
                val mesh = mMesh.map(space, entity)

                AlexandriaAPI.meshes[mob]?.let { meshInstance ->
                    meshInstance.meshTransform = Transform(
                        mob.location.position(),
                        collider.body?.rotation ?: Quaternion.Identity
                    )
                }
            }
        }
        else -> {}
    }
}
