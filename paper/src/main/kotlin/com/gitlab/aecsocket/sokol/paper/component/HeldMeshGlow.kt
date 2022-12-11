package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.ReloadEvent
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
        val attachAllow: NamedTextColor = NamedTextColor.WHITE,
        val attachDisallow: NamedTextColor = NamedTextColor.WHITE,
    ) : SimpleComponentProfile {
        override val componentType get() = HeldMeshGlow::class

        override fun createEmpty() = ComponentBlueprint { HeldMeshGlow(this) }
    }
}

@All(HeldMeshGlow::class, Held::class)
@After(MeshesInWorldSystem::class)
class HeldMeshGlowSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeldMeshGlow = ids.mapper<HeldMeshGlow>()
    private val mHeld = ids.mapper<Held>()
    private val mHeldAttachable = ids.mapper<HeldAttachable>()

    private fun update(held: Boolean, entity: SokolEntity) {
        val heldMeshGlow = mHeldMeshGlow.get(entity).profile
        val (hold) = mHeld.get(entity)
        val player = hold.player

        if (held) {
            entity.callSingle(MeshesInWorldInstanceSystem.Glowing(true, setOf(player)))
            val color = mHeldAttachable.getOr(entity)?.attachTo?.let { attachTo ->
                if (attachTo.allows) heldMeshGlow.attachAllow else heldMeshGlow.attachDisallow
            } ?: heldMeshGlow.default
            entity.callSingle(MeshesInWorldInstanceSystem.GlowingColor(color))
        } else {
            entity.callSingle(MeshesInWorldInstanceSystem.Glowing(false, setOf(player)))
        }
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        update(event.held, entity)
    }

    @Subscribe
    fun on(event: HeldAttachableSystem.ChangeAttachTo, entity: SokolEntity) {
        update(true, entity)
    }

    @Subscribe
    fun on(event: ReloadEvent, entity: SokolEntity) {
        update(true, entity)
    }
}
