package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.gitlab.aecsocket.sokol.paper.transientComponent
import net.minecraft.nbt.CompoundTag
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object Hosted {
    fun init(ctx: Sokol.InitContext) {
        ctx.transientComponent<IsWorld>()
        ctx.transientComponent<IsChunk>()
        ctx.transientComponent<IsMob>()
        ctx.transientComponent<IsBlock>()
        ctx.transientComponent<IsItem>()
        ctx.transientComponent<InItemTag>()
        ctx.system { MobConstructorSystem(ctx.sokol, it) }
        ctx.system { MobSystem(it) }
        ctx.system { BlockPersistSystem(it) }
        ctx.system { ItemPersistSystem(ctx.sokol, it) }
        ctx.system { ItemTagPersistSystem(it) }
    }
}

data class IsWorld(val world: World) : SokolComponent {
    override val componentType get() = IsWorld::class
}

data class IsChunk(val chunk: Chunk) : SokolComponent {
    override val componentType get() = IsChunk::class
}

data class IsMob(val mob: Entity) : SokolComponent {
    override val componentType get() = IsMob::class

    var lastPosition: Vector3? = null
}

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

data class InItemTag(val nmsTag: CompoundTag) : SokolComponent {
    override val componentType get() = InItemTag::class
}

@All(IsMob::class)
@Before(RemovablePreTarget::class, PlayerTrackedTarget::class)
class MobConstructorSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mIsMob = ids.mapper<IsMob>()
    private val mPlayerTracked = ids.mapper<PlayerTracked>()
    private val mRemovable = ids.mapper<Removable>()
    private val mRotation = ids.mapper<Rotation>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val mob = mIsMob.get(entity).mob

        mPlayerTracked.set(entity, PlayerTracked { mob.trackedPlayers })

        var removed = false

        mRemovable.set(entity, object : Removable {
            override val removed get() = removed || !mob.isValid

            override fun remove(silent: Boolean) {
                removed = true
                if (silent) {
                    // no remove event will be called
                    sokol.resolver.untrackMob(mob)
                }
                mob.remove()
            }
        })

        mPositionAccess.set(entity, PositionAccess(
            mob.world,
            Transform(mob.location.position(), mRotation.getOr(entity)?.rotation ?: Quaternion.Identity)
        ))
    }
}

@All(IsMob::class, PositionAccess::class)
class MobSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsMob = ids.mapper<IsMob>()
    private val mPositionAccess = ids.mapper<PositionAccess>()
    private val mRotation = ids.mapper<Rotation>()

    @Subscribe
    fun on(event: Composite.Attach, entity: SokolEntity) {
        mIsMob.remove(entity)
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val isMob = mIsMob.get(entity)
        val positionAccess = mPositionAccess.get(entity)
        val rotation = mRotation.getOr(entity)
        val mob = isMob.mob

        val transform = isMob.lastPosition?.let { lastPosition ->
            val delta = mob.location.position() - lastPosition
            positionAccess.transform.copy(position = positionAccess.transform.position + delta).also {
                positionAccess.transform = it
            }
        } ?: positionAccess.transform

        @Suppress("UnstableApiUsage")
        mob.teleport(transform.position.location(mob.world), true)
        rotation?.rotation = transform.rotation
        isMob.lastPosition = transform.position
    }
}

@All(IsBlock::class)
class BlockPersistSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsBlock = ids.mapper<IsBlock>()

    @Subscribe
    fun on(event: WriteEvent, entity: SokolEntity) {
        val isBlock = mIsBlock.get(entity)
        if (!isBlock.dirty) return

        isBlock.state.update()
    }
}

@All(IsItem::class, InTag::class)
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

@All(IsItem::class, InItemTag::class)
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
