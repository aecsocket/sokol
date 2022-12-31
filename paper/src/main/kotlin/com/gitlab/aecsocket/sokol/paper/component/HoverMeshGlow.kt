package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HoverMeshGlow(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hover_mesh_glow")
        val Type = ComponentType.deserializing(Key, Profile::class)
    }

    override val componentType get() = HoverMeshGlow::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val color: NamedTextColor = NamedTextColor.WHITE
    ) : SimpleComponentProfile<HoverMeshGlow> {
        override val componentType get() = HoverMeshGlow::class

        override fun createEmpty() = ComponentBlueprint { HoverMeshGlow(this) }
    }
}

@All(HoverMeshGlow::class, MeshesInWorld::class)
class HoverMeshGlowSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHoverMeshGlow = ids.mapper<HoverMeshGlow>()

    @Subscribe
    fun on(event: EntityHolding.ChangeHoverState, entity: SokolEntity) {
        val hoverMeshGlow = mHoverMeshGlow.get(entity).profile

        if (event.hovered) {
            entity.callSingle(MeshesInWorldInstanceSystem.Glowing(true, setOf(event.player)))
            entity.callSingle(MeshesInWorldInstanceSystem.GlowingColor(hoverMeshGlow.color))
        } else {
            entity.callSingle(MeshesInWorldInstanceSystem.Glowing(false, setOf(event.player)))
        }
    }
}
