package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.ParticleEngineEffect
import com.gitlab.aecsocket.alexandria.paper.SoundEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HeldAttachableEffects(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("held_attachable_effects")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HeldAttachableEffects::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val sound: SoundEngineEffect = SoundEngineEffect.Empty,
        val particle: ParticleEngineEffect = ParticleEngineEffect.Empty
    ) : SimpleComponentProfile {
        override val componentType get() = HeldAttachableEffects::class

        override fun createEmpty() = ComponentBlueprint { HeldAttachableEffects(this) }
    }
}

@All(HeldAttachableEffects::class, HeldAttachable::class)
class HeldAttachableEffectsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeldAttachableEffects = ids.mapper<HeldAttachableEffects>()
    private val mHeldAttachable = ids.mapper<HeldAttachable>()
    private val mPositionRead = ids.mapper<PositionRead>()

    @Subscribe
    fun on(event: HeldAttachableInputsSystem.AttachTo, entity: SokolEntity) {
        val heldAttachableEffects = mHeldAttachableEffects.get(entity).profile
        val attachTo = mHeldAttachable.get(entity).attachTo ?: return

        val location = mPositionRead.getOr(attachTo.target)?.location() ?: return
        AlexandriaAPI.soundEngine.play(location, heldAttachableEffects.sound)
        AlexandriaAPI.particleEngine.spawn(location, heldAttachableEffects.particle)
    }
}
