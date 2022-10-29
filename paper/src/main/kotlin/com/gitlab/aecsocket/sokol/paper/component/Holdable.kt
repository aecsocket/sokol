package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.give
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.entity.Player

data class Holdable(
    val settings: EntityHolding.StateSettings,
    var inUse: Boolean = false,
) : SokolComponent {
    override val componentType get() = Holdable::class
}

@All(Meshes::class, CompositeTransform::class)
@After(MeshesSystem::class, CompositeTransformSystem::class)
class HoldableBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: Build, entity: SokolEntity) {
        val compositeTransform = mCompositeTransform.get(entity)

        val transform = compositeTransform.transform
        val (parts) = entity.call(MeshesSystem.Create(
            ArrayList(),
            event.rootTransform + transform
        ) { setOf(event.player) })

        parts.forEach { (mesh, childTransform) ->
            event.parts.add(EntityHolding.Part(mesh, transform + childTransform))
        }

        mComposite.forward(entity, event)

        /*println("compose:")
        mComposite.getOr(entity)?.allChildren()?.forEach { (path, child) ->
            println(" $path: $child")
            (child as SokolEngine.EntityImpl).callResults.forEach { (event, systemResults) ->
                println("   > ${event::class}: ${systemResults.filter { it.executed }.map { it.system::class.simpleName }}")
                println()
            }
            println("\n")
        }*/
    }

    data class Build(
        val parts: MutableList<EntityHolding.Part>,
        val player: Player,
        val rootTransform: Transform
    ) : SokolEvent
}

object HoldableTarget : SokolSystem

@All(Holdable::class, HostedByItem::class)
@After(HoldableTarget::class)
class HoldableItemSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mHoldable = mappers.componentMapper<Holdable>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val player = event.player

        if (event.isRightClick && event.isShiftClick) {
            event.cancel()

            // todo a proper transform here
            val worldTransform = Transform(player.location.position())
            val (parts) = entity.call(HoldableBuildSystem.Build(
                ArrayList(),
                player,
                worldTransform
            ))

            if (parts.isEmpty()) return

            player.closeInventory()
            sokol.entityHolding.enter(player.alexandria, EntityHolding.State(
                holdable.settings,
                event.backing.slot,
                player.alexandria.acquireLock(PlayerLock.RaiseHand),
                worldTransform,
                parts
            ))
        }
    }
}

@All(Holdable::class, HostedByMob::class)
@Before(OnInputSystem::class)
@After(HoldableTarget::class, HostedByMobTarget::class)
class HoldableMobSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Take = SokolAPI.key("holdable_mob/take")
    }

    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mMob = mappers.componentMapper<HostedByMob>()
    private val mAsItem = mappers.componentMapper<HostableByItem>()
    private val mHovered = mappers.componentMapper<Hovered>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val composites = compositeMapperFor(mappers)

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val mob = mMob.get(entity).mob

        event.addAction(Take) { (_, player, cancel) ->
            // todo
            /*println("taking... ${mLookedAt.has(entity)} / ${mCollider.has(entity)}")
            val lookedAt = mLookedAt.getOr(entity) ?: return@addAction
            val collider = mCollider.getOr(entity) ?: return@addAction

            val childIdx = lookedAt.rayTestResult.triangleIndex()
            if (childIdx != -1) {
                collider.body?.compositeMap?.get(childIdx)?.let { hitPath ->
                    println("hit child path = $hitPath")
                    composites.child(entity, hitPath)?.let { hitChild ->
                        println("hit child = $hitChild")
                    }
                }
            }*/

            if (holdable.inUse) return@addAction
            val lookedAt = mHovered.getOr(entity) ?: return@addAction
            val collider = mCollider.getOr(entity) ?: return@addAction
            val asItem = mAsItem.getOr(entity) ?: return@addAction
            val compositeMap = collider.body?.compositeMap ?: return@addAction

            cancel()
            holdable.inUse = true

            val childIdx = lookedAt.rayTestResult.triangleIndex()
            if (childIdx != -1) {
                val hitPath = if (compositeMap.isEmpty()) emptyCompositePath() else compositeMap[childIdx]
                val removedEntity: SokolEntity? = if (hitPath.isEmpty()) {
                    mob.remove()
                    entity
                } else {
                    val nHitPath = hitPath.toMutableList()
                    val last = nHitPath.removeLast()
                    val parent = composites.child(entity, nHitPath) ?: return@addAction

                    entity.call(Composite.TreeMutate)
                    composites.composite(parent)?.children?.remove(last)?.also { child ->
                        child.call(SokolEvent.Remove)
                    }
                }

                removedEntity?.let {
                    if (!mAsItem.has(entity)) return@addAction
                    val item = sokol.entityHoster.hostItem(removedEntity.toBlueprint())
                    player.give(item)
                }
            }
        }
    }
}
