package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.entity.Player

data class Holdable(
    val settings: EntityHolding.StateSettings,
    var removed: Boolean = false,
) : SokolComponent {
    override val componentType get() = Holdable::class
}

@All(Meshes::class, CompositeTransform::class)
@After(MeshesSystem::class, CompositeTransformSystem::class)
class HoldableBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: BuildParts, entity: SokolEntity) {
        val compositeTransform = mCompositeTransform.get(entity)

        val (parts) = entity.call(MeshesSystem.Create(
            ArrayList(),
            event.rootTransform + compositeTransform.transform
        ) { setOf(event.player) })

        parts.forEach { (mesh, transform) ->
            event.parts.add(EntityHolding.Part(mesh, transform))
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

    data class BuildParts(
        val parts: MutableList<EntityHolding.Part>,
        val rootTransform: Transform,
        val player: Player,
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
            val (parts) = entity.call(HoldableBuildSystem.BuildParts(
                ArrayList(),
                worldTransform,
                player
            ))

            if (parts.isEmpty()) return

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
@After(HoldableTarget::class)
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

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val mob = mMob.get(entity).mob

        event.addAction(Take) { (_, player, cancel) ->
            if (holdable.removed) return@addAction
            mAsItem.getOr(entity)?.let {
                cancel()

                holdable.removed = true
                mob.remove()

                val item = sokol.entityHoster.hostItem(entity.toBlueprint())
                player.inventory.addItem(item)
            }
        }
    }
}
