package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.ParticleEngineEffect
import com.gitlab.aecsocket.alexandria.paper.SoundEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HeldAttachableEffects(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("held_attachable_effects")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { HeldAttachableEffectsSystem(it) }
        }
    }

    override val componentType get() = HeldAttachableEffects::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val sound: SoundEngineEffect = SoundEngineEffect.Empty,
        val particle: ParticleEngineEffect = ParticleEngineEffect.Empty
    ) : SimpleComponentProfile<HeldAttachableEffects> {
        override val componentType get() = HeldAttachableEffects::class

        override fun createEmpty() = ComponentBlueprint { HeldAttachableEffects(this) }
    }
}

@All(HeldAttachableEffects::class, HeldAttachable::class)
class HeldAttachableEffectsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeldAttachableEffects = ids.mapper<HeldAttachableEffects>()
    private val mHeldAttachable = ids.mapper<HeldAttachable>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    @Subscribe
    fun on(event: HeldAttachableSystem.AttachTo, entity: SokolEntity) {
        val heldAttachableEffects = mHeldAttachableEffects.get(entity).profile
        val attachTo = mHeldAttachable.get(entity).attachTo ?: return

        val location = mPositionAccess.getOr(attachTo.target)?.location() ?: return
        AlexandriaAPI.sounds.play(heldAttachableEffects.sound, location)
        AlexandriaAPI.particles.spawn(heldAttachableEffects.particle, location)
    }
}
