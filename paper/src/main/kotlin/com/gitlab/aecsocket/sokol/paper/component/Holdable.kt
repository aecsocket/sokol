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
import com.gitlab.aecsocket.sokol.paper.*
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
    private val mPosition = mappers.componentMapper<PositionWrite>()
    private val mCollider = mappers.componentMapper<Collider>()

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val position = mPosition.get(entity)
        val collider = mCollider.get(entity)
        val holdProfile = holdable.profile
        val holdState = holdable.state ?: return
        val body = collider.body?.body?.body ?: return
        val player = holdState.player

        holdState.entity = entity

        position.transform = holdState.transform
        if (body is PhysicsRigidBody) {
            CraftBulletAPI.executePhysics {
                // TODO set velocities so that it moves to the position, rather than just transform
                body.linearVelocity = Vector3f.ZERO
                body.angularVelocity = Vector3f.ZERO
            }
        }

        holdState.drawShape?.let { drawShape ->
            val transform = position.transform.bullet()
            val drawable = CraftBulletAPI.drawableOf(player, CraftBullet.DrawType.SHAPE)
            CraftBulletAPI.drawPointsShape(drawShape).forEach {
                drawable.draw(transform.transform(it))
            }
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

/*
@All(Holdable::class, HostedByMob::class, PositionWrite::class)
@Before(OnInputSystem::class)
@After(HoldableTarget::class, HostedByMobTarget::class, ColliderSystem::class)
class HoldableMobSystem2(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    companion object {
        val HoldStart = SokolAPI.key("holdable_mob/hold_start")
        val HoldStop = SokolAPI.key("holdable_mob/hold_stop")
        val HoldToggle = SokolAPI.key("holdable_mob/hold_toggle")
        val Take = SokolAPI.key("holdable_mob/take")
    }

    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mMob = mappers.componentMapper<HostedByMob>()
    private val mPositionWrite = mappers.componentMapper<PositionWrite>()
    private val mAsItem = mappers.componentMapper<HostableByItem>()
    private val mHovered = mappers.componentMapper<Hovered>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mComposite = mappers.componentMapper<Composite>()
    private val mEntitySlot = mappers.componentMapper<EntitySlot>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mSupplierEntityAccess = mappers.componentMapper<SupplierEntityAccess>()
    private val mCompositePathed = mappers.componentMapper<CompositePathed>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val mob = mMob.get(entity).mob

        holdable.holdState = sokol.entityHolding.heldBy[mob.uniqueId]
    }

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val position = mPositionWrite.get(entity)
        val mob = mMob.get(entity).mob

        fun start(player: Player) {
            sokol.entityHolding.start(player.alexandria, mob, position.transform)
        }

        fun stop(player: Player) {
            val holdState = holdable.holdState ?: return
            if (holdState.placeState.valid) {
                sokol.entityHolding.stop(player.alexandria)
                holdState.attachTo?.let { attachTo ->
                    holdable.inUse = true

                    sokol.scheduleDelayed {
                        sokol.persistence.removeTag(mob.persistentDataContainer, sokol.persistence.entityKey)
                        mob.remove()
                    }

                    sokol.useMob(attachTo.mob) { attachToEntity ->
                        val parentEntity = mComposite.child(attachToEntity, attachTo.path) ?: return@useMob
                        val composite = mComposite.getOr(parentEntity) ?: return@useMob

                        composite.attach(ENTITY_SLOT_CHILD_KEY, parentEntity, entity)
                        attachToEntity.call(Composite.TreeMutate)
                    }
                }
            }
        }

        event.addAction(HoldStart) { (player, _, cancel) ->
            if (holdable.inUse) return@addAction
            cancel()
            start(player)
        }

        event.addAction(HoldStop) { (player, _, cancel) ->
            if (holdable.inUse) return@addAction
            cancel()
            stop(player)
        }

        event.addAction(HoldToggle) { (player, _, cancel) ->
            if (holdable.inUse) return@addAction
            cancel()
            if (holdable.holdState == null) start(player) else stop(player)
        }

        event.addAction(Take) { (player, _, cancel) ->
            if (holdable.inUse) return@addAction
            val collider = mCollider.getOr(entity) ?: return@addAction
            cancel()
            holdable.inUse = true

            // if we're not holding it, we have a `hovered` which tells us which part was taken
            // else, we take the whole thing
            val hitPath = mHovered.getOr(entity)?.let { colliderCompositeHitPath(collider, it.rayTestResult) } ?: emptyCompositePath()
            val removedEntity: SokolEntity = if (hitPath.isEmpty()) {
                sokol.scheduleDelayed {
                    mob.remove()
                }
                entity
            } else {
                val nHitPath = hitPath.toMutableList()
                val last = nHitPath.removeLast()
                val parent = mComposite.child(entity, nHitPath) ?: return@addAction
                val parentComposite = mComposite.getOr(parent) ?: return@addAction
                val child = parentComposite[last] ?: return@addAction
                if (!mAsItem.has(child)) return@addAction

                // TODO some entities shouldn't allow taking children out of them
                parentComposite.remove(last)
                entity.call(Composite.TreeMutate)
                child
            }

            if (!mAsItem.has(removedEntity)) return@addAction
            removedEntity.call(SokolEvent.Remove)
            val item = sokol.entityHoster.hostItem(removedEntity.toBlueprint())
            player.give(item)
        }
    }

    private fun holdState(entity: SokolEntity, state: Boolean) {
        val holdable = mHoldable.get(entity)
        val collider = mCollider.getOr(entity)
        val player = holdable.holdState?.player ?: return

        val glowEvent = MeshesInWorldSystem.Glow(state, setOf(player))
        entity.call(glowEvent)
        mComposite.forward(entity, glowEvent)

        val body = collider?.body?.body
        if (body is PhysicsRigidBody)
            body.isKinematic = state
    }

    @Subscribe
    fun on(event: StartHold, entity: SokolEntity) {
        holdState(entity, true)
    }

    @Subscribe
    fun on(event: StopHold, entity: SokolEntity) {
        holdState(entity, false)
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val position = mPositionWrite.get(entity)
        val collider = mCollider.get(entity)
        val holdState = holdable.holdState ?: return
        val (tracked) = collider.body ?: return
        val body = tracked.body
        val player = holdState.player
        val settings = holdable.profile.settings

        position.transform = holdState.transform

        if (body is PhysicsRigidBody) {
            CraftBulletAPI.executePhysics {
                //body.transform = holdState.transform.bullet()
                body.linearVelocity = Vector3f.ZERO
                body.angularVelocity = Vector3f.ZERO

//                val transform = holdState.transform
//
//                val currentPos = body.physPosition
//                val targetPos = transform.translation.bullet()
//                body.linearVelocity = (targetPos - currentPos) * 5f
//
//                val currentAng = body.physRotation.mult(Vector3f(0f, 0f, 1f), null)
//                val targetAng = (transform.rotation * Vector3.Forward).bullet()
//                body.angularVelocity = (targetAng - currentAng) * 5f
            }
        }

        val from = player.eyeLocation
        val direction = from.direction.alexandria()
        val snapDistance = settings.snapDistance

        data class SlotBody(
            val mob: Entity,
            val entitySlot: EntitySlot,
            val path: CompositePath,
            val transform: Transform,
            val tIn: Double
        )

        val slotBodies = ArrayList<SlotBody>()
        val location = player.eyeLocation
        val ray = Ray(location.position(), location.direction())
        location.getNearbyEntities(snapDistance, snapDistance, snapDistance).forEach { mob ->
            sokol.useMob(mob, false) { entity ->
                fun addBody(entity: SokolEntity) {
                    val entitySlot = mEntitySlot.getOr(entity) ?: return
                    val path = mCompositePathed.getOr(entity)?.path ?: return
                    val transform = mPositionRead.getOr(entity)?.transform ?: return
                    val collision = testRayShape(transform.invert(ray), entitySlot.profile.shape) ?: return

                    slotBodies.add(SlotBody(mob, entitySlot, path, transform, collision.tIn))
                }

                fun walk(entity: SokolEntity) {
                    addBody(entity)
                    mComposite.forEachChild(entity) { (_, child) ->
                        walk(child)
                    }
                }

                walk(entity)
            }
        }

        val slotResult = slotBodies.minByOrNull { it.tIn }

        CraftBulletAPI.executePhysics {
            if (!holdState.frozen) {
                val fSnapDistance = snapDistance.toFloat()
                val result = player.rayTestFrom(fSnapDistance)
                    .firstOrNull {
                        val obj = it.collisionObject
                        obj !is TrackedPhysicsObject || obj.id != collider.bodyData?.bodyId
                    }

                fun setDefaultTransform() {
                    holdState.transform = if (result == null) {
                        Transform(
                            (from + direction * settings.holdDistance).position(),
                            from.rotation()
                        ) + settings.placeTransform
                    } else {
                        val hitPos = from.position() + direction * (snapDistance * result.hitFraction)

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

                        Transform(hitPos, rotation) + settings.placeTransform
                    }
                }

                var placeState = HoldPlaceState.DISALLOW
                var attachTo: HoldAttachTo? = null
                if (slotResult != null && (result == null || slotResult.tIn < result.hitFraction * fSnapDistance)) {
                    if (slotResult.entitySlot.profile.accepts) {
                        placeState = HoldPlaceState.ALLOW_ATTACH
                        holdState.transform = slotResult.transform
                        attachTo = HoldAttachTo(slotResult.mob, slotResult.path)
                    } else {
                        placeState = HoldPlaceState.DISALLOW_ATTACH
                        setDefaultTransform()
                    }
                } else {
                    if (result != null || settings.allowNonSnapPlacing) {
                        placeState = HoldPlaceState.ALLOW
                    }
                    setDefaultTransform()
                }

                holdState.attachTo = attachTo
                if (placeState != holdState.placeState) {
                    holdState.placeState = placeState
                    val glowColorEvent = MeshesInWorldSystem.GlowColor(settings.placeColors[placeState] ?: NamedTextColor.WHITE)
                    entity.call(glowColorEvent)
                    mComposite.forward(entity, glowColorEvent)
                }
            }

            holdState.drawShape?.let { drawShape ->
                val transform = position.transform.bullet()
                val drawable = CraftBulletAPI.drawableOf(player, CraftBullet.DrawType.SHAPE)
                CraftBulletAPI.drawPointsShape(drawShape).forEach {
                    drawable.draw(transform.transform(it))
                }
            }
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val holdState = holdable.holdState ?: return

        sokol.entityHolding.stop(holdState.player.alexandria)
        holdable.holdState = null
    }

    object StartHold : SokolEvent

    object StopHold : SokolEvent
}*/
