package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.HoldPlaceState
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class HoldableGlow(
    val profile: Profile,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable_glow")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoldableGlow::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val colors: Map<HoldPlaceState, NamedTextColor>
    ) : NonReadingComponentProfile {
        override fun readEmpty() = HoldableGlow(this)
    }
}

@All(HoldableGlow::class, Holdable::class, MeshesInWorld::class)
class HoldableGlowSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mHoldableGlow = mappers.componentMapper<HoldableGlow>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: HoldableSystem.ChangePlacing, entity: SokolEntity) {
        val holdableGlowing = mHoldableGlow.get(entity).profile

        val glowColor = holdableGlowing.colors[event.placing] ?: NamedTextColor.WHITE
        val glowColorEvent = MeshesInWorldSystem.GlowColor(glowColor)
        entity.call(glowColorEvent)
        mComposite.forward(entity, glowColorEvent)
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val holdableGlowing = mHoldableGlow.get(entity).profile
        val holdState = event.state

        val glowEvent = MeshesInWorldSystem.Glow(event.holding, setOf(event.player))
        val glowColor = holdableGlowing.colors[holdState.placing] ?: NamedTextColor.WHITE
        val glowColorEvent = MeshesInWorldSystem.GlowColor(glowColor)

        entity.call(glowEvent)
        entity.call(glowColorEvent)
        mComposite.forEachChild(entity) { (_, child) ->
            child.call(glowEvent)
            child.call(glowColorEvent)
        }
    }
}
