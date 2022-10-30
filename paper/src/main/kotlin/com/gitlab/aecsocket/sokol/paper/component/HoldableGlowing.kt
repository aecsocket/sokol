package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

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
        val allow: NamedTextColor = NamedTextColor.WHITE,
        val disallow: NamedTextColor = NamedTextColor.WHITE,
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
    fun on(event: HoldableSystem.ChangeAllowPlace, entity: SokolEntity) {
        val holdableGlowing = mHoldableGlowing.get(entity).profile

        val glowColorEvent = MeshesInWorldSystem.GlowColor(if (event.allow) holdableGlowing.allow else holdableGlowing.disallow)
        entity.call(glowColorEvent)
        mComposite.forward(entity, glowColorEvent)
    }

    @Subscribe
    fun on(event: HoldableSystem.HoldState, entity: SokolEntity) {
        val holdableGlowing = mHoldableGlowing.get(entity).profile

        val glowEvent = MeshesInWorldSystem.Glow(event.state, setOf(event.player))
        val glowColorEvent = MeshesInWorldSystem.GlowColor(holdableGlowing.allow)
        entity.call(glowEvent)
        entity.call(glowColorEvent)
        mComposite.forEachChild(entity) { (_, child) ->
            child.call(glowEvent)
            child.call(glowColorEvent)
        }
    }
}
