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
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*
import kotlin.math.PI
import kotlin.math.abs

data class Holdable(val profile: Profile) : MarkerPersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Holdable::class
    override val key get() = Key

    var state: HoldState? = null

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

@All(Holdable::class, PositionRead::class)
@Before(OnInputSystem::class)
@After(HoldableTarget::class, PositionTarget::class)
class HoldableSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    companion object {
        val MoveStart = SokolAPI.key("holdable/move_start")
        val RotateStart = SokolAPI.key("holdable/rotate_start")
        val Stop = SokolAPI.key("holdable/stop")
    }

    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mEntitySlot = mappers.componentMapper<EntitySlot>()
    private val mMob = mappers.componentMapper<HostedByMob>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val positionRead = mPositionRead.get(entity)
        val collider = mCollider.get(entity)
        val holdProfile = holdable.profile
        val body = collider.body?.body?.body ?: return

        AlexandriaAPI.soundEngine.play(
            positionRead.location(),
            if (event.holding) holdProfile.soundHoldStart else holdProfile.soundHoldStop
        )

        // todo this doesn't work
        if (event.holding) {
            body.removeCollideWithGroup(PhysicsCollisionObject.COLLISION_GROUP_01)
        } else {
            body.addCollideWithGroup(PhysicsCollisionObject.COLLISION_GROUP_01)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val positionRead = mPositionRead.get(entity)
        val holdState = holdable.state ?: return
        val player = holdState.player

        holdState.entity = entity

        holdState.drawShape?.let { drawShape ->
            val transform = positionRead.transform.bullet()
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
        entity.call(Update)
    }

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val positionRead = mPositionRead.get(entity)
        val mob = mMob.getOr(entity)?.mob
        val holdState = holdable.state

        event.addAction(MoveStart) { (player, _, cancel) ->
            cancel()
            if (holdState != null) return@addAction false
            sokol.entityHolding.start(player.alexandria, entity, MovingHoldOperation(positionRead.transform), mob)
            true
        }

        event.addAction(RotateStart) { (player, _, cancel) ->
            cancel()
            if (holdState != null) return@addAction false

            val planeNormal = (positionRead.transform.rotation * holdable.profile.holdTransform.rotation.inverse) * Vector3.Forward
            sokol.entityHolding.start(player.alexandria, entity, RotatingHoldOperation(
                positionRead.transform.rotation,
                planeNormal,
            ), mob)
            true
        }

        event.addAction(Stop) { (player, _, cancel) ->
            cancel()
            if (holdState == null) return@addAction false
            if (holdState.operation.canRelease) sokol.entityHolding.stop(player.alexandria)
            true
        }
    }

    object Update : SokolEvent
}

enum class MovingPlaceState(val canRelease: Boolean) {
    ALLOW           (true),
    DISALLOW        (false),
    ALLOW_ATTACH    (true),
    DISALLOW_ATTACH (true)
}

data class HoldAttach(
    val entity: SokolEntity,
    val path: CompositePath,
)

class MovingHoldOperation(
    var transform: Transform
) : HoldOperation {
    var placing: MovingPlaceState = MovingPlaceState.DISALLOW
    var attachTo: HoldAttach? = null

    override val canRelease get() = placing.canRelease
}

class RotatingHoldOperation(
    var rotation: Quaternion,
    val planeNormal: Vector3,
) : HoldOperation {
    override val canRelease get() = true
}

@All(Holdable::class, PositionWrite::class, Collider::class)
@After(HoldableTarget::class, PositionTarget::class)
class HoldableMovementSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mPositionWrite = mappers.componentMapper<PositionWrite>()
    private val mCollider = mappers.componentMapper<Collider>()

    @Subscribe
    fun on(event: HoldableSystem.Update, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val positionWrite = mPositionWrite.get(entity)
        val collider = mCollider.get(entity)
        val holdProfile = holdable.profile
        val holdState = holdable.state ?: return
        val body = collider.body?.body?.body ?: return
        val player = holdState.player

        when (val holdOp = holdState.operation) {
            is MovingHoldOperation -> {
                positionWrite.transform = holdOp.transform
                val oldPlacing = holdOp.placing

                val from = player.eyeLocation
                val direction = from.direction.alexandria()

                CraftBulletAPI.executePhysics {
                    if (body is PhysicsRigidBody) {
                        // TODO set velocities so that it moves to the position, rather than just transform
                        body.linearVelocity = Vector3f.ZERO
                        body.angularVelocity = Vector3f.ZERO
                    }

                    val result = player.rayTestFrom(holdProfile.fSnapDistance)
                        .firstOrNull {
                            val obj = it.collisionObject
                            obj !is TrackedPhysicsObject || obj.id != collider.bodyData?.bodyId
                        }

                    val transform: Transform
                    if (result == null) {
                        holdOp.placing = if (holdProfile.allowFreePlace) MovingPlaceState.ALLOW else MovingPlaceState.DISALLOW
                        transform = Transform(
                            (from + direction * holdProfile.holdDistance).position(),
                            from.rotation()
                        ) + holdProfile.holdTransform
                    } else {
                        holdOp.placing = MovingPlaceState.ALLOW
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
                    holdOp.transform = transform
                    holdOp.attachTo = null

                    entity.call(UpdatePosition)

                    if (holdOp.placing != oldPlacing) {
                        entity.call(ChangeMovingPlaceState(oldPlacing))
                    }
                }
            }
            is RotatingHoldOperation -> {
                val transform = positionWrite.transform.copy(rotation = holdOp.rotation)
                positionWrite.transform = transform

                CraftBulletAPI.executePhysics {
                    if (body is PhysicsRigidBody) {
                        // TODO set velocities so that it moves to the position, rather than just transform
                        body.linearVelocity = Vector3f.ZERO
                        body.angularVelocity = Vector3f.ZERO
                    }

                    // form a plane facing "up" from the mob
                    // e.g. if it's on a surface, this is the normal of the surface
                    val plane = PlaneShape(holdOp.planeNormal)
                    val planeOrigin = transform.translation
                    // find the intersection between where the player's looking and this plane
                    val location = player.eyeLocation
                    val position = location.position()
                    val direction = location.direction()
                    val ray = Ray(position - planeOrigin, direction)

                    // this could only have no intersection if the ray and plane are parallel
                    testRayPlane(ray, plane)?.let { (tIn) ->
                        val pointAt = position + direction * tIn

                        // make the mob look at that point
                        // TODO this doesn't work for verticals because a component of the quaternion is lost
                        holdOp.rotation = quaternionLooking((pointAt - planeOrigin).normalized, Vector3.Up)
                    }
                }
            }
        }
    }

    object UpdatePosition : SokolEvent

    data class ChangeMovingPlaceState(
        val old: MovingPlaceState
    ) : SokolEvent
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
            val blueprint = entity.toBlueprint()
            mItem.remove(blueprint)
            val mob = sokol.entityHoster.hostMob(blueprint, player.eyeLocation)
            sokol.entityHolding.stop(axPlayer)
            player.closeInventory()
            sokol.useMob(mob) { mobEntity ->
                sokol.entityHolding.start(axPlayer, mobEntity, MovingHoldOperation(player.eyeLocation.transform()), mob)
            }
        }
    }
}

@All(Holdable::class, HostedByMob::class)
@Before(HoldableTarget::class, OnInputSystem::class)
@After(HostedByMobTarget::class)
class HoldableMobSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mMob = mappers.componentMapper<HostedByMob>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val mob = mMob.get(entity).mob

        holdable.state = sokol.entityHolding.heldBy[mob.uniqueId]
    }
}
