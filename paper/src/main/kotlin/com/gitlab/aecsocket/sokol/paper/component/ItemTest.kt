package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.FLAG_FORCE_UPDATE
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

data class ItemTest(
    val dTicks: Delta<Int>
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_test")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = ItemTest::class
    override val key get() = Key

    override val dirty get() = dTicks.dirty
    var ticks by dTicks

    constructor(
        ticks: Int
    ) : this(Delta(ticks))

    override fun write(ctx: NBTTagContext) = ctx.makeInt(ticks)

    override fun writeDelta(tag: NBTTag): NBTTag {
        return dTicks.ifDirty { tag.makeInt(it) } ?: tag
    }

    override fun serialize(node: ConfigurationNode) {
        node.set(ticks)
    }

    object Profile : ComponentProfile {
        override fun read(space: SokolSpaceAccess, tag: NBTTag) = ItemTest(tag.asInt())

        override fun deserialize(space: SokolSpaceAccess, node: ConfigurationNode) = ItemTest(node.get { 0 })

        override fun createEmpty() = ItemTest(0)
    }
}

@All(ItemTest::class, IsItem::class)
class ItemTestSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mItemTest = ids.mapper<ItemTest>()
    private val mIsItem = ids.mapper<IsItem>()

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val itemTest = mItemTest.get(entity)
        val isItem = mIsItem.get(entity)

        entity.setFlag(FLAG_FORCE_UPDATE)
        itemTest.ticks += 1
        isItem.writeMeta { meta ->
            meta.displayName(Component.text("Ticks: ${itemTest.ticks}"))
        }
    }
}
