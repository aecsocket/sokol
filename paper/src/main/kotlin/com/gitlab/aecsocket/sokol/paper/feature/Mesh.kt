package com.gitlab.aecsocket.sokol.paper.feature

import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.gitlab.aecsocket.alexandria.core.extension.EulerOrder
import com.gitlab.aecsocket.alexandria.core.extension.degrees
import com.gitlab.aecsocket.alexandria.core.extension.euler
import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.alexandria.paper.sendPacket
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asTransform
import com.gitlab.aecsocket.sokol.core.extension.ofTransform
import com.gitlab.aecsocket.sokol.paper.*
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.*

private const val PARTS = "parts"
private const val ENTITY_ID = "entity_id"
private const val TRANSFORM = "transform"

data class Mesh(
    var data: MeshData? = null
) : PersistentComponent {
    @ConfigSerializable
    data class MeshData(
        @Required val parts: List<MeshPart>,
    )

    @ConfigSerializable
    data class MeshPart(
        @Required val entityId: Int,
        @Required val transform: Transform,
    )

    override val type get() = Mesh
    override val key get() = Key

    override fun write(tag: CompoundNBTTag.Mutable) {
        data?.let { mesh -> tag.set(PARTS) { ofList().apply {
            mesh.parts.forEach { part -> add { ofCompound()
                .set(ENTITY_ID) { ofInt(part.entityId) }
                .set(TRANSFORM) { ofTransform(part.transform) }
            } }
        } } }
    }

    override fun write(node: ConfigurationNode) {
        data?.let { node.set(it) }
    }

    class Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: CompoundNBTTag) = Mesh(
            if (tag.contains(PARTS)) MeshData(
                tag.get(PARTS) { asList().map { it.asCompound().let { part -> MeshPart(
                    part.get(ENTITY_ID) { asInt() },
                    part.get(TRANSFORM) { asTransform() },
                ) } } }
            ) else null
        )

        override fun read(node: ConfigurationNode) = Mesh(
            if (node.hasChild(PARTS)) node.get<MeshData>() else null
        )
    }

    companion object : ComponentType<Mesh> {
        val Key = SokolAPI.key("mesh")
    }
}

class MeshSystem(engine: SokolEngine) : SokolSystem {
    private val entFilter = engine.entityFilter(
        setOf(NBTTagAccessor, HostedByEntity, Collider, Mesh)
    )
    private val mTagAccessor = engine.componentMapper(NBTTagAccessor)
    private val mMob = engine.componentMapper(HostedByEntity)
    private val mCollider = engine.componentMapper(Collider)
    private val mMesh = engine.componentMapper(Mesh)

    private fun transform(mob: Entity, collider: Collider, part: Mesh.MeshPart): Transform {
        return Transform(
            mob.location.position(),
            collider.body?.rotation ?: Quaternion.Identity
        ) + part.transform
    }

    private fun position(transform: Transform) = transform.translation.y { it - 1.45 }.run {
        Vector3d(x, y, z)
    }

    private fun headRotation(transform: Transform) = transform.rotation.euler(EulerOrder.ZYX).degrees.x { -it }.run {
        Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
    }

    override fun handle(space: SokolEngine.Space, event: SokolEvent) = when (event) {
        is ByEntityEvent.Added -> {
            space.entitiesBy(entFilter).forEach { entity ->
                val tagAccessor = mTagAccessor.map(space, entity)
                val mesh = mMesh.map(space, entity)

                if (mesh.data == null) {
                    val entityId = bukkitNextEntityId
                    mesh.data = Mesh.MeshData(
                        listOf(Mesh.MeshPart(entityId, Transform.Identity))
                    )
                    tagAccessor.write(mesh)
                }
            }
        }
        is ByEntityEvent.Shown -> {
            val player = event.backing.player as Player

            space.entitiesBy(entFilter).forEach { entity ->
                event.backing.isCancelled = true
                val mob = mMob.map(space, entity).entity
                val collider = mCollider.map(space, entity)
                val mesh = mMesh.map(space, entity)

                mesh.data?.let { meshData ->
                    meshData.parts.forEach { part ->
                        val transform = transform(mob, collider, part)
                        val position = position(transform)
                        val headRotation = headRotation(transform)

                        val entityId = part.entityId
                        player.sendPacket(WrapperPlayServerSpawnEntity(entityId,
                            Optional.of(UUID.randomUUID()), EntityTypes.ARMOR_STAND,
                            position, 0f, 0f, 0f, 0, Optional.empty(),
                        ))

                        player.sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                            EntityData(0, EntityDataTypes.BYTE, (0x20).toByte()),
                            EntityData(15, EntityDataTypes.BYTE, (0x10).toByte()),
                            EntityData(16, EntityDataTypes.ROTATION, headRotation),
                        )))

                        player.sendPacket(WrapperPlayServerEntityEquipment(entityId, listOf(
                            Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(ItemStack(Material.IRON_NUGGET).withMeta { meta -> meta.setCustomModelData(1) }))
                        )))
                    }
                }
            }
        }
        is ByEntityEvent.Hidden -> {
            val player = event.backing.player as Player

            space.entitiesBy(entFilter).forEach { entity ->
                event.thisEntityCancelled = true
                val mesh = mMesh.map(space, entity)

                println("removed entity ${mesh.data?.parts?.get(0)?.entityId}")

                mesh.data?.let { meshData ->
                    val entityIds = meshData.parts.map { it.entityId }
                    player.sendPacket(WrapperPlayServerDestroyEntities(*entityIds.toIntArray()))
                }
            }
        }
        is UpdateEvent -> {
            space.entitiesBy(entFilter).forEach { entity ->
                val mob = mMob.map(space, entity).entity
                val collider = mCollider.map(space, entity)
                val mesh = mMesh.map(space, entity)

                mesh.data?.let { meshData ->
                    val packets = meshData.parts.flatMap { part ->
                        val transform = transform(mob, collider, part)
                        val position = position(transform)
                        val headRotation = headRotation(transform)

                        val entityId = part.entityId
                        listOf(
                            WrapperPlayServerEntityTeleport(entityId,
                                position, 0f, 0f, false),
                            WrapperPlayServerEntityMetadata(entityId, listOf(
                                EntityData(16, EntityDataTypes.ROTATION, headRotation)
                            ))
                        )
                    }

                    mob.trackedPlayers.forEach { player ->
                        packets.forEach { player.sendPacket(it) }
                    }
                }
            }
        }
        else -> {}
    }
}
