package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.PaperCompoundTag
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
    object Target : SokolSystem

    override val componentType get() = IsWorld::class
}

data class IsChunk(val chunk: Chunk) : SokolComponent {
    object Target : SokolSystem

    override val componentType get() = IsChunk::class
}

data class IsMob(val mob: Entity) : SokolComponent {
    object Target : SokolSystem

    override val componentType get() = IsMob::class
}

data class IsBlock(
    val block: Block,
    internal val getState: () -> BlockState
) : SokolComponent {
    object Target : SokolSystem

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
    object Target : SokolSystem

    object FormTarget : SokolSystem

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

/*
@All(IsMob::class)
@Before(IsMob.Target::class, SupplierTrackedPlayersTarget::class, PositionTarget::class)
class MobInjectorSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mMob = mappers.componentMapper<IsMob>()
    private val mRotation = mappers.componentMapper<Rotation>()
    private val mSupplierEntityAccess = mappers.componentMapper<SupplierEntityAccess>()
    private val mSupplierTrackedPlayers = mappers.componentMapper<SupplierTrackedPlayers>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mPositionWrite = mappers.componentMapper<PositionWrite>()
    private val mRemovable = mappers.componentMapper<Removable>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val mob = mMob.get(entity).mob

        mSupplierTrackedPlayers.set(entity, object : SupplierTrackedPlayers {
            override val trackedPlayers: () -> Set<Player> get() = { mob.trackedPlayers }
        })

        mSupplierEntityAccess.set(entity, object : SupplierEntityAccess {
            override fun useEntity(builder: (EntityBlueprint) -> Unit, consumer: (SokolEntity) -> Unit) {
                sokol.useMob(mob, builder = builder, consumer = consumer)
            }
        })

        var removed = false
        mRemovable.set(entity, object : Removable {
            override val removed get() = removed || !mob.isValid

            override fun remove() {
                removed = true
                sokol.scheduleDelayed {
                    sokol.persistence.removeTag(mob.persistentDataContainer, sokol.persistence.entityKey)
                    mob.remove()
                }
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
}*/
