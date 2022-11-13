package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.ParticleEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class ParticleEffectSpawner(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("particle_effect_spawner")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = ParticleEffectSpawner::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val effect: ParticleEngineEffect,
        val velocityMultiplier: Double = 0.0
    ) : SimpleComponentProfile {
        override val componentType get() = ParticleEffectSpawner::class

        override fun createEmpty() = ParticleEffectSpawner(this)
    }
}

@All(ParticleEffectSpawner::class, PositionRead::class)
@After(PositionTarget::class, VelocityTarget::class)
class ParticleEffectSpawnerSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mParticleEffectSpawner = ids.mapper<ParticleEffectSpawner>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mVelocityRead = ids.mapper<VelocityRead>()

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val particleEffectSpawner = mParticleEffectSpawner.get(entity).profile
        val positionRead = mPositionRead.get(entity)
        val velocityRead = mVelocityRead.getOr(entity)

        val velocity = velocityRead?.linear ?: Vector3.Zero

        val effect = ParticleEngineEffect(particleEffectSpawner.effect.effects.map { effect ->
            if (effect.count.compareTo(0.0) == 0) effect.copy(
                size = velocity * particleEffectSpawner.velocityMultiplier
            ) else effect
        })

        AlexandriaAPI.particleEngine.spawn(positionRead.location(), effect)
    }
}
