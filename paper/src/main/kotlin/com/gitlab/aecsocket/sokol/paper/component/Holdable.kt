package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.entity.Player

data class Holdable(
    val settings: EntityHolding.StateSettings,
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

@All(Holdable::class, HostedByItem::class)
class HoldableSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mHoldable = mappers.componentMapper<Holdable>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntity) {
        val placeable = mHoldable.get(entity)
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
                placeable.settings,
                entity,
                event.backing.slot,
                player.alexandria.acquireLock(PlayerLock.RaiseHand),
                worldTransform,
                parts
            ))
        }
    }
}
