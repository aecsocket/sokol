package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class MeshesHoverGlow(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("meshes_hover_glow")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = MeshesHoverGlow::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val color: NamedTextColor = NamedTextColor.WHITE
    ) : SimpleComponentProfile {
        override val componentType get() = MeshesHoverGlow::class

        override fun createEmpty() = ComponentBlueprint { MeshesHoverGlow(this) }
    }
}

@All(MeshesHoverGlow::class, MeshesInWorld::class)
class MeshesHoverGlowSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mMeshesHoverGlow = ids.mapper<MeshesHoverGlow>()

    @Subscribe
    fun on(event: EntityHolding.ChangeHoverState, entity: SokolEntity) {
        val meshesHoverGlow = mMeshesHoverGlow.get(entity).profile

        if (event.hovered) {
            entity.callSingle(MeshesInWorldSystem.Glowing(true, setOf(event.player)))
            entity.callSingle(MeshesInWorldSystem.GlowingColor(meshesHoverGlow.color))
        } else {
            entity.callSingle(MeshesInWorldSystem.Glowing(false, setOf(event.player)))
        }
    }
}
