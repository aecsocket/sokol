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

data class HeldCompositeEffects(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("held_composite_effects")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { HeldCompositeEffectsSystem(it) }
        }
    }

    override val componentType get() = HeldCompositeEffects::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val attachSound: SoundEngineEffect = SoundEngineEffect.Empty,
        val attachParticle: ParticleEngineEffect = ParticleEngineEffect.Empty,
        val detachSound: SoundEngineEffect = SoundEngineEffect.Empty,
        val detachParticle: ParticleEngineEffect = ParticleEngineEffect.Empty,
    ) : SimpleComponentProfile<HeldCompositeEffects> {
        override val componentType get() = HeldCompositeEffects::class

        override fun createEmpty() = ComponentBlueprint { HeldCompositeEffects(this) }
    }
}

@All(HeldCompositeEffects::class, PositionAccess::class)
class HeldCompositeEffectsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeldCompositeEffects = ids.mapper<HeldCompositeEffects>()
    private val mHeldAttachable = ids.mapper<HeldAttachable>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    @Subscribe
    fun on(event: HeldAttachableSystem.AttachTo, entity: SokolEntity) {
        val heldCompositeEffects = mHeldCompositeEffects.get(entity).profile
        val location = mPositionAccess.get(
            mHeldAttachable.getOr(entity)?.attachTo?.target ?: entity
        ).location()

        AlexandriaAPI.sounds.play(heldCompositeEffects.attachSound, location)
        AlexandriaAPI.particles.spawn(heldCompositeEffects.attachParticle, location)
    }

    @Subscribe
    fun on(event: HoldDetachableSystem.DetachFrom, entity: SokolEntity) {
        val heldCompositeEffects = mHeldCompositeEffects.get(entity).profile
        val location = mPositionAccess.get(
            mHeldAttachable.getOr(entity)?.attachTo?.target ?: entity
        ).location()

        AlexandriaAPI.sounds.play(heldCompositeEffects.detachSound, location)
        AlexandriaAPI.particles.spawn(heldCompositeEffects.detachParticle, location)
    }
}
