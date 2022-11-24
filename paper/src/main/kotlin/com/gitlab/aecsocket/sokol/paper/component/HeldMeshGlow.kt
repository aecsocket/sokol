package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HeldMeshGlow(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("held_mesh_glow")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HeldMeshGlow::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val default: NamedTextColor = NamedTextColor.WHITE,
    ) : SimpleComponentProfile {
        override val componentType get() = HeldMeshGlow::class

        override fun createEmpty() = ComponentBlueprint { HeldMeshGlow(this) }
    }
}

@All(HeldMeshGlow::class, Held::class)
class HeldMeshGlowSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeldMeshGlow = ids.mapper<HeldMeshGlow>()
    private val mHeld = ids.mapper<Held>()

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val heldMeshGlow = mHeldMeshGlow.get(entity).profile
        val (hold) = mHeld.get(entity)
        val player = hold.player

        if (event.held) {
            entity.callSingle(MeshesInWorldSystem.Glowing(true, setOf(player)))
            entity.callSingle(MeshesInWorldSystem.GlowingColor(heldMeshGlow.default))
        } else {
            entity.callSingle(MeshesInWorldSystem.Glowing(false, setOf(player)))
        }
    }
}
