package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.Euler3
import com.gitlab.aecsocket.alexandria.core.extension.EulerOrder
import com.gitlab.aecsocket.alexandria.core.extension.quaternion
import com.gitlab.aecsocket.alexandria.core.extension.radians
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBullet
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.rayTestFrom
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.core.extension.collisionOf
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import org.bukkit.GameMode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*
import kotlin.math.PI
import kotlin.math.abs

data class Holdable(
    val profile: Profile,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Holdable::class
    override val key get() = Key

    var state: HoldState? = null

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val holdTransform: Transform = Transform.Identity,
        val holdDistance: Double = 0.0,
        val snapDistance: Double = 0.0,
        val allowFreePlace: Boolean = true,
        val soundHoldStart: SoundEngineEffect = SoundEngineEffect.Empty,
        val soundHoldStop: SoundEngineEffect = SoundEngineEffect.Empty,
    ) : NonReadingComponentProfile {
        val fSnapDistance = snapDistance.toFloat()

        override fun readEmpty() = Holdable(this)
    }
}

object HoldableTarget : SokolSystem

@All(Holdable::class, PositionWrite::class, Collider::class)
@Before(OnInputSystem::class)
@After(HoldableTarget::class, PositionTarget::class)
class HoldableSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mPositionWrite = mappers.componentMapper<PositionWrite>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mEntitySlot = mappers.componentMapper<EntitySlot>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val positionWrite = mPositionWrite.get(entity)
        val collider = mCollider.get(entity)
        val holdProfile = holdable.profile
        val body = collider.body?.body?.body ?: return

        AlexandriaAPI.soundEngine.play(
            positionWrite.location(),
            if (event.holding) holdProfile.soundHoldStart else holdProfile.soundHoldStop
        )

        if (event.holding) {
            body.removeCollideWithGroup(PhysicsCollisionObject.COLLISION_GROUP_01)
        } else {
            body.addCollideWithGroup(PhysicsCollisionObject.COLLISION_GROUP_01)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val positionWrite = mPositionWrite.get(entity)
        val collider = mCollider.get(entity)
        val holdProfile = holdable.profile
        val holdState = holdable.state ?: return
        val body = collider.body?.body?.body ?: return
        val player = holdState.player

        holdState.entity = entity

        positionWrite.transform = holdState.transform
        if (body is PhysicsRigidBody) {
            CraftBulletAPI.executePhysics {
                // TODO set velocities so that it moves to the position, rather than just transform
                body.linearVelocity = Vector3f.ZERO
                body.angularVelocity = Vector3f.ZERO
            }
        }

        holdState.drawShape?.let { drawShape ->
            val transform = positionWrite.transform.bullet()
            val drawable = CraftBulletAPI.drawableOf(player, CraftBullet.DrawType.SHAPE)
            CraftBulletAPI.drawPointsShape(drawShape).forEach {
                drawable.draw(transform.transform(it))
            }
        }

        if (holdState.drawSlotShapes) {
            val drawable = CraftBulletAPI.drawableOf(player, CraftBullet.DrawType.SHAPE)

            fun act(entity: SokolEntity) {
                val entitySlot = mEntitySlot.getOr(entity) ?: return
                val slotPosition = mPositionRead.getOr(entity) ?: return
                val shape = collisionOf(entitySlot.profile.shape)
                val transform = slotPosition.transform.bullet()
                CraftBulletAPI.drawPointsShape(shape).forEach {
                    drawable.draw(transform.transform(it))
                }
            }

            fun walk(entity: SokolEntity) {
                act(entity)
                mComposite.forEachChild(entity) { (_, child) ->
                    walk(child)
                }
            }

            walk(entity)
        }

        if (holdState.frozen) return
        val from = player.eyeLocation
        val direction = from.direction.alexandria()

        CraftBulletAPI.executePhysics {
            val result = player.rayTestFrom(holdProfile.fSnapDistance)
                .firstOrNull {
                    val obj = it.collisionObject
                    obj !is TrackedPhysicsObject || obj.id != collider.bodyData?.bodyId
                }

            val transform: Transform
            val placing: HoldPlaceState
            if (result == null) {
                placing = if (holdProfile.allowFreePlace) HoldPlaceState.ALLOW else HoldPlaceState.DISALLOW
                transform = Transform(
                    (from + direction * holdProfile.holdDistance).position(),
                    from.rotation()
                ) + holdProfile.holdTransform
            } else {
                placing = HoldPlaceState.ALLOW
                val hitPos = from.position() + direction * (holdProfile.snapDistance * result.hitFraction)

                // the hit normal is facing from the surface, to the player
                // but when holding (non-snap) it's the opposite direction
                // so we invert the normal here to face it in the right direction
                val dir = -result.hitNormal.alexandria()
                val rotation = if (abs(dir.dot(Vector3.Up)) > 0.99) {
                    // `dir` and `up` are (close to) collinear
                    val yaw = player.location.yaw.radians.toDouble()
                    // `rotation` will be facing "away" from the player
                    quaternionFromTo(Vector3.Forward, dir) *
                            Euler3(z = (if (dir.y > 0.0) -yaw else yaw) + 2 * PI).quaternion(EulerOrder.XYZ)
                } else {
                    val v1 = Vector3.Up.cross(dir).normalized
                    val v2 = dir.cross(v1).normalized
                    quaternionOfAxes(v1, v2, dir)
                }

                transform = Transform(hitPos, rotation) + holdProfile.holdTransform
            }
            holdState.transform = transform
            holdState.attachTo = null

            val (nPlacing) = entity.call(ComputeState(placing))

            if (nPlacing != holdState.placing) {
                holdState.placing = nPlacing
                entity.call(ChangePlacing(nPlacing))
            }
        }
    }

    data class ChangePlacing(val placing: HoldPlaceState) : SokolEvent

    data class ComputeState(var placing: HoldPlaceState) : SokolEvent
}

@All(Holdable::class, HostedByItem::class, HostableByMob::class)
@After(HoldableTarget::class)
class HoldableItemSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mItem = mappers.componentMapper<HostedByItem>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntity) {
        val item = mItem.get(entity)
        val player = event.player

        if (event.isRightClick && event.isShiftClick) {
            event.cancel()

            if (player.gameMode != GameMode.CREATIVE) {
                item.item.subtract()
            }

            val axPlayer = player.alexandria
            val mob = sokol.entityHoster.hostMob(entity.toBlueprint(), player.eyeLocation)
            sokol.entityHolding.stop(axPlayer)
            player.closeInventory()
            sokol.useMob(mob) { mobEntity ->
                sokol.entityHolding.start(axPlayer, mobEntity, player.eyeLocation.transform(), mob)
            }
        }
    }
}

@All(Holdable::class, HostedByMob::class, PositionWrite::class)
@Before(HoldableTarget::class, OnInputSystem::class)
@After(HostedByMobTarget::class)
class HoldableMobSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    companion object {
        val HoldStart = SokolAPI.key("holdable_mob/hold_start")
        val HoldStop = SokolAPI.key("holdable_mob/hold_stop")
    }

    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mMob = mappers.componentMapper<HostedByMob>()
    private val mPosition = mappers.componentMapper<PositionWrite>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val mob = mMob.get(entity).mob

        holdable.state = sokol.entityHolding.heldBy[mob.uniqueId]
    }

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val mob = mMob.get(entity).mob
        val position = mPosition.get(entity)
        val holdState = holdable.state

        event.addAction(HoldStart) { (player, _, cancel) ->
            cancel()
            if (holdState != null) return@addAction false
            sokol.entityHolding.start(player.alexandria, entity, position.transform, mob)
            true
        }

        event.addAction(HoldStop) { (player, _, cancel) ->
            cancel()
            if (holdState == null) return@addAction false
            if (holdState.placing.valid) sokol.entityHolding.stop(player.alexandria)
            true
        }
    }
}
