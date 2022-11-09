package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class HoldableGlow(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable_glow")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoldableGlow::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val colors: Map<MovingPlaceState, NamedTextColor>
    ) : SimpleComponentProfile {
        override fun readEmpty() = HoldableGlow(this)
    }
}

@All(HoldableGlow::class, Holdable::class, MeshesInWorld::class)
class HoldableGlowSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mHoldableGlow = mappers.componentMapper<HoldableGlow>()
    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: HoldableMovementSystem.ChangeMovingPlaceState, entity: SokolEntity) {
        val holdableGlow = mHoldableGlow.get(entity).profile
        val holdable = mHoldable.get(entity)
        val holdOp = holdable.state?.operation as? MovingHoldOperation ?: return

        val glowColor = holdableGlow.colors[holdOp.placing] ?: NamedTextColor.WHITE
        val glowColorEvent = MeshesInWorldSystem.GlowingColor(glowColor)
        mComposite.forwardAll(entity, glowColorEvent)
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val holdableGlow = mHoldableGlow.get(entity).profile
        // `holdable` hasn't had its `state` assigned here yet
        val holdOp = event.state.operation

        val placeState = (holdOp as? MovingHoldOperation)?.placing
            ?: if (holdOp.canRelease) MovingPlaceState.ALLOW else MovingPlaceState.DISALLOW
        val glowColor = holdableGlow.colors[placeState] ?: NamedTextColor.WHITE

        val glowEvent = MeshesInWorldSystem.Glowing(event.holding, setOf(event.player))
        val glowColorEvent = MeshesInWorldSystem.GlowingColor(glowColor)
        mComposite.forAllEntities(entity) { target ->
            target.call(glowEvent)
            target.call(glowColorEvent)
        }
    }
}
