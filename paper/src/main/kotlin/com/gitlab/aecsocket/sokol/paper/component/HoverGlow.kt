package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHover
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.util.colliderHitPath
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HoverGlow(
    val profile: Profile,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("hover_glow")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoverGlow::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val color: NamedTextColor = NamedTextColor.WHITE,
    ) : NonReadingComponentProfile {
        override fun readEmpty() = HoverGlow(this)
    }
}

@All(HoverGlow::class, MeshesInWorld::class)
class HoverGlowSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mHoverGlow = mappers.componentMapper<HoverGlow>()

    @Subscribe
    fun on(event: ChangeState, entity: SokolEntity) {
        val hoverGlow = mHoverGlow.get(entity).profile
        val playerSet = setOf(event.player)

        if (event.hovered) {
            entity.call(MeshesInWorldSystem.Glow(true, playerSet))
            entity.call(MeshesInWorldSystem.GlowColor(hoverGlow.color))
        } else {
            entity.call(MeshesInWorldSystem.Glow(false, playerSet))
        }
    }

    data class ChangeState(
        val player: Player,
        val hovered: Boolean
    ) : SokolEvent
}

class HoverGlowCallerSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mCollider = mappers.componentMapper<Collider>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: EntityHover.HoverState, entity: SokolEntity) {
        val glowEntity = mComposite.child(entity, colliderHitPath(mCollider.getOr(entity), event.testResult)) ?: return
        glowEntity.call(HoverGlowSystem.ChangeState(event.player, event.hovered))
    }

    @Subscribe
    fun on(event: EntityHover.ChangeHoverIndex, entity: SokolEntity) {
        val collider = mCollider.getOr(entity)

        mComposite.child(entity, colliderHitPath(collider, event.oldIndex))
            ?.call(HoverGlowSystem.ChangeState(event.player, false))
        mComposite.child(entity, colliderHitPath(collider, event.newIndex))
            ?.call(HoverGlowSystem.ChangeState(event.player, true))
    }
}
