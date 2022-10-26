package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.PlayerLock
import com.gitlab.aecsocket.alexandria.paper.acquireLock
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*

data class Placeable(
    val placeTransform: Transform,
    val holdDistance: Double,
    val snapDistance: Double,
) : SokolComponent {
    override val componentType get() = Placeable::class
}

@All(Placeable::class, HostedByItem::class)
class PlaceableSystem(
    private val sokol: Sokol,
    engine: SokolEngine
) : SokolSystem {
    private val mPlaceable = engine.componentMapper<Placeable>()
    private val mItem = engine.componentMapper<HostedByItem>()
    private val mComposite = engine.componentMapper<Composite>()
    private val mSlotTransform = engine.componentMapper<SlotTransform>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntity) {
        val placeable = mPlaceable.map(entity)
        val item = mItem.map(entity)
        val player = event.player

        if (event.isRightClick && event.isShiftClick) {
            event.cancel()

            val parts = ArrayList<ItemPlacing.Part>()
            val rootTransform = Transform.Identity
            val worldTransform = Transform(player.location.position())
            val tracked = setOf(player)
            parts.add(ItemPlacing.Part(
                AlexandriaAPI.meshes.create(item.item, worldTransform, { tracked }, false),
                rootTransform
            ))

            fun walkComposite(composite: Composite, parent: Transform) {
                composite.children.forEach { (_, child) ->
                    val transform = parent + (mSlotTransform.mapOr(child)?.transform ?: Transform.Identity)

                    val itemBlueprint = child.toBlueprint()
                    itemBlueprint.components.set(ItemHolder.byMob(player))
                    sokol.entityHoster.hostItemOr(itemBlueprint)?.let { item ->
                        parts.add(ItemPlacing.Part(
                            AlexandriaAPI.meshes.create(item, worldTransform, { tracked }, false),
                            transform
                        ))
                    }
                    mComposite.mapOr(child)?.let { walkComposite(it, transform) }
                }
            }

            mComposite.mapOr(entity)?.let { walkComposite(it, rootTransform) }

            sokol.itemPlacing.enter(player.alexandria, ItemPlacing.State(
                event.backing.slot,
                player.alexandria.acquireLock(PlayerLock.RaiseHand),
                parts,
                placeable.placeTransform,
                placeable.holdDistance,
                placeable.snapDistance,
            ))
        }
    }
}
