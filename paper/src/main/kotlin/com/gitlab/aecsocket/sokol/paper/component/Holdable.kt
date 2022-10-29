package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.Euler3
import com.gitlab.aecsocket.alexandria.core.extension.EulerOrder
import com.gitlab.aecsocket.alexandria.core.extension.quaternion
import com.gitlab.aecsocket.alexandria.core.extension.radians
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.core.physics.quaternionFromTo
import com.gitlab.aecsocket.alexandria.core.physics.quaternionOfAxes
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
import com.gitlab.aecsocket.sokol.paper.util.colliderCompositeHitPath
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.util.*
import kotlin.math.PI
import kotlin.math.abs

private const val HOLDER_ID = "holder_id"

enum class HoldPlaceState(val valid: Boolean) {
    ALLOW           (true),
    ALLOW_ATTACH    (true),
    DISALLOW        (false),
    DISALLOW_ATTACH (true),
}

@ConfigSerializable
data class HoldSettings(
    val placeTransform: Transform = Transform.Identity,
    val holdDistance: Double = 0.0,
    val snapDistance: Double = 0.0,
    val allowNonSnapPlacing: Boolean = true,
    val placeColors: Map<HoldPlaceState, NamedTextColor> = emptyMap(),
)

data class Holdable(
    val profile: Profile,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Holdable::class
    override val key get() = Key

    var inUse = false
    var holdState: EntityHolding.State? = null

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .setOrClear(HOLDER_ID) { holdState?.player?.uniqueId?.let { makeUUID(it) } }

    override fun write(node: ConfigurationNode) {
        node.node(HOLDER_ID).set(holdState?.player?.uniqueId)
    }

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val settings: HoldSettings
    ) : NonReadingComponentProfile {
        override fun readEmpty() = Holdable(this)
    }
}

object HoldableTarget : SokolSystem

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
            sokol.entityHolding.start(axPlayer, mob, player.eyeLocation.transform())
            player.closeInventory()
        }
    }
}

@All(Holdable::class, HostedByMob::class, PositionWrite::class)
@Before(OnInputSystem::class)
@After(HoldableTarget::class, HostedByMobTarget::class, ColliderSystem::class)
class HoldableMobSystem(
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

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val mob = mMob.get(entity).mob

        holdable.holdState = sokol.entityHolding.mobToState[mob.uniqueId]
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
                        mob.remove()
                    }

                    attachTo.useEntity { parentEntity ->
                        mComposite.get(parentEntity).children[attachTo.key] = entity
                        parentEntity.call(Composite.TreeMutate)
                    }
                }
            }
        }

        event.addAction(HoldStart) { (_, player, cancel) ->
            if (holdable.inUse) return@addAction
            cancel()
            start(player)
        }

        event.addAction(HoldStop) { (_, player, cancel) ->
            if (holdable.inUse) return@addAction
            cancel()
            stop(player)
        }

        event.addAction(HoldToggle) { (_, player, cancel) ->
            if (holdable.inUse) return@addAction
            cancel()
            if (holdable.holdState == null) start(player) else stop(player)
        }

        event.addAction(Take) { (_, player, cancel) ->
            if (holdable.inUse) return@addAction
            val collider = mCollider.getOr(entity) ?: return@addAction
            cancel()
            holdable.inUse = true

            // if we're not holding it, we have a `hovered` which tells us which part was taken
            // else, we take the whole thing
            val hitPath = mHovered.getOr(entity)?.let { colliderCompositeHitPath(collider, it.rayTestResult) } ?: emptyCompositePath()
            val removedEntity: SokolEntity? = if (hitPath.isEmpty()) {
                sokol.scheduleDelayed {
                    mob.remove()
                }
                entity
            } else {
                val nHitPath = hitPath.toMutableList()
                val last = nHitPath.removeLast()
                val parent = mComposite.child(entity, nHitPath) ?: return@addAction
                val parentChildren = mComposite.getOr(parent)?.children ?: return@addAction
                val child = parentChildren[last] ?: return@addAction
                if (!mAsItem.has(child)) return@addAction

                // TODO some entities shouldn't allow taking children out of them
                parentChildren.remove(last)
                entity.call(Composite.TreeMutate)
                child
            }

            removedEntity?.let {
                if (!mAsItem.has(removedEntity)) return@addAction
                removedEntity.call(SokolEvent.Remove)
                val item = sokol.entityHoster.hostItem(removedEntity.toBlueprint())
                player.give(item)
            }
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

        CraftBulletAPI.executePhysics {
            if (!holdState.frozen) {
                val result = player.rayTestFrom(snapDistance.toFloat())
                    .firstOrNull {
                        val obj = it.collisionObject
                        obj !is TrackedPhysicsObject || obj.id != collider.bodyData?.bodyId
                    }

                var placeState = HoldPlaceState.DISALLOW
                var attachTo: EntityHolding.AttachTo? = null
                holdState.transform = if (result == null) {
                    if (settings.allowNonSnapPlacing)
                        placeState = HoldPlaceState.ALLOW
                    Transform(
                        (from + direction * settings.holdDistance).position(),
                        from.rotation()
                    ) + settings.placeTransform
                } else {
                    fun default(setState: Boolean = true): Transform {
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

                        if (setState) placeState = HoldPlaceState.ALLOW
                        return Transform(hitPos, rotation) + settings.placeTransform
                    }

                    fun transform(): Transform {
                        val obj = result.collisionObject as? SokolPhysicsObject ?: return default()
                        val hitEntity = obj.entity
                        val hitEntitySupplier = mSupplierEntityAccess.getOr(hitEntity) ?: return default()
                        val hitPath = colliderCompositeHitPath(mCollider.getOr(hitEntity), result)
                        val slotEntity = mComposite.child(hitEntity, hitPath) ?: return default()

                        val entitySlot = mEntitySlot.getOr(slotEntity) ?: return default()
                        val composite = mComposite.getOr(slotEntity) ?: return default()
                        val slotPosition = mPositionRead.getOr(slotEntity) ?: return default()
                        if (composite.children.isNotEmpty()) return default()

                        return if (entitySlot.profile.accepts) {
                            placeState = HoldPlaceState.ALLOW_ATTACH
                            attachTo = EntityHolding.AttachTo(ENTITY_SLOT_CHILD_KEY) { entityConsumer ->
                                hitEntitySupplier.useEntity { newEntity ->
                                    mComposite.child(newEntity, hitPath)?.let(entityConsumer)
                                }
                            }
                            slotPosition.transform
                        } else {
                            placeState = HoldPlaceState.DISALLOW_ATTACH
                            default(false)
                        }
                    }

                    transform()
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
                CraftBulletAPI.drawOperationFor(drawShape, position.transform.bullet())
                    .invoke(CraftBulletAPI.drawableOf(player, CraftBullet.DrawType.SHAPE))
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
}
