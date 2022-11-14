package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.TPS
import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import net.minecraft.nbt.CompoundTag
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

data class IsWorld(val world: World) : SokolComponent {
    override val componentType get() = IsWorld::class
}

object IsWorldTarget : SokolSystem

data class IsChunk(val chunk: Chunk) : SokolComponent {
    override val componentType get() = IsChunk::class
}

object IsChunkTarget : SokolSystem

data class IsMob(val mob: Entity) : SokolComponent {
    override val componentType get() = IsMob::class
}

object IsMobTarget : SokolSystem

data class IsBlock(
    val block: Block,
    internal val getState: () -> BlockState
) : SokolComponent {
    override val componentType get() = IsBlock::class

    internal var dirty = false
    internal val state by lazy(getState)

    fun <R> readState(action: (BlockState) -> R): R {
        return action(state)
    }

    fun writeState(action: (BlockState) -> Unit) {
        action(state)
        dirty = true
    }

    override fun toString() = "IsBlock(block=$block)"
}

object IsBlockTarget : SokolSystem

data class IsItem(
    private val getItem: () -> ItemStack,
    private val getMeta: () -> ItemMeta
) : SokolComponent {
    override val componentType get() = IsItem::class

    internal var dirty = false
    val item by lazy(getItem)
    internal val meta by lazy(getMeta)

    fun <R> readMeta(action: (ItemMeta) -> R): R {
        return action(meta)
    }

    fun writeMeta(action: (ItemMeta) -> Unit) {
        action(meta)
        dirty = true
    }

    override fun toString() = "IsItem(item=$item)"
}

object IsItemTarget : SokolSystem

object IsItemFormTarget : SokolSystem

data class InItemTag(val nmsTag: CompoundTag) : SokolComponent {
    override val componentType get() = InItemTag::class
}

@All(IsBlock::class, IsRoot::class)
class BlockPersistSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsBlock = ids.mapper<IsBlock>()

    @Subscribe
    fun on(event: WriteEvent, entity: SokolEntity) {
        val isBlock = mIsBlock.get(entity)
        if (!isBlock.dirty) return

        isBlock.state.update()
    }
}

@All(IsItem::class, InTag::class, IsRoot::class)
class ItemPersistSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mIsItem = ids.mapper<IsItem>()
    private val mInTag = ids.mapper<InTag>()

    @Subscribe
    fun on(event: WriteEvent, entity: SokolEntity) {
        val isItem = mIsItem.get(entity)
        val inTag = mInTag.get(entity)
        if (!isItem.dirty) return

        val meta = isItem.meta
        sokol.persistence.writeTagTo(inTag.tag, sokol.persistence.entityKey, meta.persistentDataContainer)
        isItem.item.itemMeta = isItem.meta
    }
}

@All(IsItem::class, InItemTag::class, IsRoot::class)
@After(ItemPersistSystem::class)
class ItemTagPersistSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsItem = ids.mapper<IsItem>()
    private val mInItemTag = ids.mapper<InItemTag>()

    @Subscribe
    fun on(event: WriteEvent, entity: SokolEntity) {
        val isItem = mIsItem.get(entity)
        val inItemTag = mInItemTag.get(entity)
        if (!isItem.dirty) return

        (isItem.item as CraftItemStack).handle.save(inItemTag.nmsTag)
    }

}

@All(IsMob::class)
@Before(IsMobTarget::class, RemovableTarget::class, PlayerTrackedTarget::class, PositionTarget::class)
class MobConstructorSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsMob = ids.mapper<IsMob>()
    private val mPlayerTracked = ids.mapper<PlayerTracked>()
    private val mRemovable = ids.mapper<Removable>()
    private val mRotation = ids.mapper<Rotation>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mPositionWrite = ids.mapper<PositionWrite>()
    private val mVelocityRead = ids.mapper<VelocityRead>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val mob = mIsMob.get(entity).mob

        mPlayerTracked.set(entity, object : PlayerTracked {
            override fun trackedPlayers() = mob.trackedPlayers
        })

        mRemovable.set(entity, object : Removable {
            override val removed get() = !mob.isValid
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
                    rotation?.rotation = transform.rotation
                    mob.teleport(value.translation.location(world), true)
                }
        })

        mVelocityRead.set(entity, object : VelocityRead {
            override val linear get() = mob.velocity.alexandria() * TPS.toDouble()
        })
    }
}
