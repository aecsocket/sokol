package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.writeEntityTagTo
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

interface HostedByWorld : SokolComponent {
    override val componentType get() = HostedByWorld::class

    val world: World
}

interface HostedByChunk : SokolComponent {
    override val componentType get() = HostedByChunk::class

    val chunk: Chunk
}

interface HostedByMob : SokolComponent {
    override val componentType get() = HostedByMob::class

    val mob: Entity
}

interface HostedByBlock : SokolComponent {
    override val componentType get() = HostedByBlock::class

    val block: Block

    fun <R> readState(action: (BlockState) -> R): R

    fun writeState(action: (BlockState) -> Unit)
}

interface HostedByItem : SokolComponent {
    override val componentType get() = HostedByItem::class

    val item: ItemStack

    fun <R> readMeta(action: (ItemMeta) -> R): R

    fun writeMeta(action: (ItemMeta) -> Unit)
}

fun hostedByWorld(world: World) = object : HostedByWorld {
    override val world get() = world

    override fun toString() = "HostedByWorld(${world.name})"
}

fun hostedByChunk(chunk: Chunk) = object : HostedByChunk {
    override val chunk get() = chunk

    override fun toString() = "HostedByChunk(${chunk.world.name}: ${chunk.x}, ${chunk.z})"
}

fun hostedByMob(mob: Entity) = object : HostedByMob {
    override val mob get() = mob

    override fun toString() = "HostedByMob($mob)"
}

fun hostedByItem(item: ItemStack, meta: ItemMeta) = object : HostedByItem {
    override val item get() = item

    override fun <R> readMeta(action: (ItemMeta) -> R): R {
        return action(meta)
    }

    override fun writeMeta(action: (ItemMeta) -> Unit) {
        action(meta)
    }

    override fun toString() = "HostedByItem($item)"
}

object HostedByWorldTarget : SokolSystem

object HostedByChunkTarget : SokolSystem

object HostedByMobTarget : SokolSystem

object HostedByBlockTarget : SokolSystem

object HostedByItemFormTarget : SokolSystem

@After(HostedByItemFormTarget::class)
object HostedByItemTarget : SokolSystem

@All(HostedByMob::class)
@Before(HostedByMobTarget::class)
class MobInjectorSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mMob = mappers.componentMapper<HostedByMob>()
    private val mRotation = mappers.componentMapper<Rotation>()
    private val mSupplierIsValid = mappers.componentMapper<SupplierIsValid>()
    private val mSupplierTrackedPlayers = mappers.componentMapper<SupplierTrackedPlayers>()
    private val mSupplierEntityAccess = mappers.componentMapper<SupplierEntityAccess>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mPositionWrite = mappers.componentMapper<PositionWrite>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val mob = mMob.get(entity).mob

        mSupplierIsValid.set(entity, object : SupplierIsValid {
            override val valid: () -> Boolean get() = { mob.isValid }
        })

        mSupplierTrackedPlayers.set(entity, object : SupplierTrackedPlayers {
            override val trackedPlayers: () -> Set<Player> get() = { mob.trackedPlayers }
        })

        mSupplierEntityAccess.set(entity, object : SupplierEntityAccess {
            override fun useEntity(consumer: (SokolEntity) -> Unit) {
                consumer(entity)
                sokol.persistence.writeEntityTagTo(entity, mob.persistentDataContainer)
            }
        })

        val rotation = mRotation.getOr(entity)
        var transform = Transform(mob.location.position(), rotation?.rotation ?: Quaternion.Identity)

        mPositionRead.set(entity, object : PositionRead {
            override val world get() = mob.world

            override val transform get() = transform
        })

        mPositionWrite.set(entity, object : PositionWrite {
            override val world get() = mob.world

            @Suppress("UnstableApiUsage")
            override var transform: Transform
                get() = transform
                set(value) {
                    transform = value
                    rotation?.rotation = value.rotation
                    mob.teleport(value.translation.location(world), true)
                }
        })
    }
}
