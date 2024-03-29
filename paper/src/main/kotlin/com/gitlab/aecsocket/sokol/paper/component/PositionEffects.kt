package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.ParticleEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class PositionEffects(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("position_effects")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { PositionEffectsSystem(it) }
        }
    }

    override val componentType get() = PositionEffects::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val particle: ParticleEngineEffect = ParticleEngineEffect.Empty,
        val particleVelocityMultiplier: Double = 0.0
    ) : SimpleComponentProfile<PositionEffects> {
        override val componentType get() = PositionEffects::class

        override fun createEmpty() = ComponentBlueprint { PositionEffects(this) }
    }
}

@All(PositionEffects::class, PositionAccess::class)
@After(PositionAccessTarget::class, VelocityAccessTarget::class)
class PositionEffectsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mPositionEffects = ids.mapper<PositionEffects>()
    private val mPositionAccess = ids.mapper<PositionAccess>()
    private val mVelocityAccess = ids.mapper<VelocityAccess>()

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val positionEffects = mPositionEffects.get(entity).profile
        val positionAccess = mPositionAccess.get(entity)
        val velocityAccess = mVelocityAccess.getOr(entity)

        val velocity = velocityAccess?.linear ?: Vector3.Zero
        val particle = positionEffects.particle.map { effect ->
            if (effect.count.compareTo(0.0) == 0) effect.copy(
                size = velocity * positionEffects.particleVelocityMultiplier
            ) else effect
        }

        val location = positionAccess.location()
        AlexandriaAPI.particles.spawn(particle, location)
    }
}
