package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode

class Placeable : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("placeable")
    }

    override val componentType get() = Placeable::class.java
    override val key get() = Key

    lateinit var placeTransform: Transform

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    object Type : PersistentComponentType {
        override val componentType get() = Placeable::class.java
        override val key get() = Key

        override fun read(tag: NBTTag) = Placeable()

        override fun read(node: ConfigurationNode) = Placeable()

        override fun readFactory(node: ConfigurationNode) = PersistentComponentFactory { Placeable() }
    }
}

@All(Placeable::class, HostedByItem::class)
class PlaceableSystem(
    private val sokol: Sokol,
    engine: SokolEngine
) : SokolSystem {
    private val mPlaceable = engine.componentMapper<Placeable>()
    private val mItem = engine.componentMapper<HostedByItem>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntityAccess) {
        val placeable = mPlaceable.map(entity)
        val item = mItem.map(entity)
        val player = event.player

        if (event.isRightClick && event.isShiftClick) {
            event.cancel()
            sokol.itemPlacing.enter(player.alexandria, ItemPlacing.State(
                event.backing.slot,
                listOf(
                    AlexandriaAPI.meshes.create(
                        item.stack,
                        Transform(
                            player.eyeLocation.position()
                        ),
                        { setOf(player) },
                        false
                    )
                ),
                placeable.placeTransform
            ))
        }
    }
}
