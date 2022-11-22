package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.RangeMapFloat
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.ParticleEngineEffect
import com.gitlab.aecsocket.alexandria.paper.SoundEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.BlockRigidBody
import com.gitlab.aecsocket.craftbullet.paper.location
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.math.Vector3f
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class ColliderEffects(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider_effects")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    data class ContactData(
        val thisBody: PhysicsCollisionObject,
        val otherBody: PhysicsCollisionObject,
        val impulse: Float,
        val position: Vector3f
    )

    override val componentType get() = ColliderEffects::class
    override val key get() = Key

    var nextContact: ContactData? = null

    @ConfigSerializable
    data class Profile(
        val impulseThreshold: Float = 1.0f,
        val sound: SoundEngineEffect = SoundEngineEffect.Empty,
        val soundVolumeMap: RangeMapFloat = RangeMapFloat.Identity,
        val particleBlock: ParticleEngineEffect = ParticleEngineEffect.Empty,
        val particleBlockCountMap: RangeMapFloat = RangeMapFloat.Identity,
    ) : SimpleComponentProfile {
        override val componentType get() = ColliderEffects::class

        override fun createEmpty() = ComponentBlueprint { ColliderEffects(this) }
    }
}

@All(ColliderEffects::class, ColliderInstance::class, PositionRead::class)
@Before(ColliderInstanceTarget::class, PositionTarget::class)
class ColliderEffectsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderEffects = ids.mapper<ColliderEffects>()
    private val mPositionRead = ids.mapper<PositionRead>()

    @Subscribe
    fun on(event: ColliderSystem.Contact, entity: SokolEntity) {
        val colliderEffects = mColliderEffects.get(entity)

        val point = event.point
        val impulse = point.impulse
        if (impulse >= colliderEffects.profile.impulseThreshold) {
            colliderEffects.nextContact = ColliderEffects.ContactData(
                event.thisBody, event.otherBody, impulse, event.point.positionWorldA
            )
        }
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        // only run one impulse effect set per update
        val colliderEffects = mColliderEffects.get(entity)
        val position = mPositionRead.get(entity)

        val (_, otherBody, impulse, worldPos) = colliderEffects.nextContact ?: return
        colliderEffects.nextContact = null

        val world = position.world
        val location = worldPos.location(world)

        val volume = colliderEffects.profile.soundVolumeMap.map(impulse)
        AlexandriaAPI.soundEngine.play(location, colliderEffects.profile.sound.map { effect ->
            effect.copy(volume = effect.sound.volume() * volume)
        })

        if (otherBody is BlockRigidBody) {
            val (bx, by, bz) = otherBody.pos
            val block = world.getBlockAt(bx, by, bz)

            val blockData = block.blockData
            val count = colliderEffects.profile.particleBlockCountMap.map(impulse)
            AlexandriaAPI.particleEngine.spawn(location, colliderEffects.profile.particleBlock.map { effect ->
                effect.copy(data = blockData, count = effect.count * count)
            })
        }
    }
}
