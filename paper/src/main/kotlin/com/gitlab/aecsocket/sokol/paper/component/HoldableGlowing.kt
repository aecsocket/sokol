package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.HoldPlaceState
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class HoldableGlowing(
    val profile: Profile,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable_glowing")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoldableGlowing::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val colors: Map<HoldPlaceState, NamedTextColor>
    ) : NonReadingComponentProfile {
        override fun readEmpty() = HoldableGlowing(this)
    }
}

@All(HoldableGlowing::class, Holdable::class, MeshesInWorld::class)
@After(MeshesInWorldSystem::class)
class HoldableGlowingSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mHoldableGlowing = mappers.componentMapper<HoldableGlowing>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: HoldableSystem.ChangePlacing, entity: SokolEntity) {
        val holdableGlowing = mHoldableGlowing.get(entity).profile

        val glowColor = holdableGlowing.colors[event.placing] ?: NamedTextColor.WHITE
        val glowColorEvent = MeshesInWorldSystem.GlowColor(glowColor)
        entity.call(glowColorEvent)
        mComposite.forward(entity, glowColorEvent)
    }

    @Subscribe
    fun on(event: HoldableSystem.ChangeHoldState, entity: SokolEntity) {
        val glowEvent = MeshesInWorldSystem.Glow(event.state, setOf(event.player))
        entity.call(glowEvent)
        mComposite.forEachChild(entity) { (_, child) ->
            child.call(glowEvent)
        }
    }
}
