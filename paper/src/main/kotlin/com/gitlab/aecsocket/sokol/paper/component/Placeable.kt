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

@All(Meshes::class, CompositeTransform::class)
@After(CompositeTransformSystem::class)
class PlaceableBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mMeshes = mappers.componentMapper<Meshes>()
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: BuildParts, entity: SokolEntity) {
        val meshes = mMeshes.get(entity)
        val compositeTransform = mCompositeTransform.get(entity)



        mComposite.forward(entity, event)
    }

    class BuildParts(

    ) : SokolEvent
}

@All(Placeable::class, HostedByItem::class)
class PlaceableSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mPlaceable = mappers.componentMapper<Placeable>()
    private val mItem = mappers.componentMapper<HostedByItem>()
    private val mComposite = mappers.componentMapper<Composite>()
    private val mLocalTransform = mappers.componentMapper<LocalTransform>()
    private val mItemHolder = mappers.componentMapper<ItemHolder>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntity) {
        val placeable = mPlaceable.get(entity)
        val item = mItem.get(entity)
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
                    val transform = parent + (mLocalTransform.getOr(child)?.transform ?: Transform.Identity)

                    val itemBlueprint = child.toBlueprint()
                    mItemHolder.set(itemBlueprint, ItemHolder.byMob(player))
                    sokol.entityHoster.hostItemOr(itemBlueprint)?.let { item ->
                        parts.add(ItemPlacing.Part(
                            AlexandriaAPI.meshes.create(item, worldTransform, { tracked }, false),
                            transform
                        ))
                    }
                    mComposite.getOr(child)?.let { walkComposite(it, transform) }
                }
            }

            mComposite.getOr(entity)?.let { walkComposite(it, rootTransform) }

            sokol.itemPlacing.enter(player.alexandria, ItemPlacing.State(
                entity,
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
