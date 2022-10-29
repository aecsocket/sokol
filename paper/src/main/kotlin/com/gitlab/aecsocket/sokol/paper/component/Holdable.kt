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
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.util.*
import kotlin.math.PI
import kotlin.math.abs

private const val HOLDER_ID = "holder_id"

data class Holdable(
    val profile: Profile,
    var holdState: EntityHolding.State?,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Holdable::class
    override val key get() = Key

    var inUse = false

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .setOrClear(HOLDER_ID) { holdState?.player?.uniqueId?.let { makeUUID(it) } }

    override fun write(node: ConfigurationNode) {
        node.node(HOLDER_ID).set(holdState?.player?.uniqueId)
    }

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val settings: EntityHolding.HoldSettings
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound().run { Holdable(this@Profile,
            getOr(HOLDER_ID) { Bukkit.getPlayer(asUUID())?.alexandria?.entityHolding }) }

        override fun read(node: ConfigurationNode) = Holdable(this,
            node.node(HOLDER_ID).get<UUID>()?.let { Bukkit.getPlayer(it)?.alexandria?.entityHolding })

        override fun readEmpty() = Holdable(this, null)
    }
}

object HoldableTarget : SokolSystem

@All(Holdable::class, HostedByItem::class, HostableByMob::class)
@After(HoldableTarget::class)
class HoldableItemSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mItem = mappers.componentMapper<HostedByItem>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val item = mItem.get(entity)
        val player = event.player

        if (event.isRightClick && event.isShiftClick) {
            event.cancel()

            if (player.gameMode != GameMode.CREATIVE) {
                item.item.subtract()
            }

            holdable.holdState = sokol.entityHolding.start(player.alexandria)
            sokol.entityHoster.hostMob(entity.toBlueprint(), player.eyeLocation)
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
        val Hold = SokolAPI.key("holdable_mob/hold")
        val Take = SokolAPI.key("holdable_mob/take")
    }

    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mMob = mappers.componentMapper<HostedByMob>()
    private val mPosition = mappers.componentMapper<PositionWrite>()
    private val mAsItem = mappers.componentMapper<HostableByItem>()
    private val mHovered = mappers.componentMapper<Hovered>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val position = mPosition.get(entity)
        val mob = mMob.get(entity).mob

        event.addAction(Hold) { (_, player, cancel) ->
            if (holdable.inUse) return@addAction
            cancel()

            holdable.holdState = sokol.entityHolding.start(player.alexandria, position.transform)
        }

        event.addAction(Take) { (_, player, cancel) ->
            if (holdable.inUse) return@addAction
            val hovered = mHovered.getOr(entity) ?: return@addAction
            val collider = mCollider.getOr(entity) ?: return@addAction
            cancel()
            holdable.inUse = true

            val hitPath = colliderCompositeHitPath(collider, hovered.rayTestResult)
            val removedEntity: SokolEntity? = if (hitPath.isEmpty()) {
                mob.remove()
                entity
            } else {
                val nHitPath = hitPath.toMutableList()
                val last = nHitPath.removeLast()
                val parent = mComposite.child(entity, nHitPath) ?: return@addAction

                entity.call(Composite.TreeMutate)
                mComposite.getOr(parent)?.children?.remove(last)
            }

            removedEntity?.let {
                removedEntity.call(SokolEvent.Remove)
                if (!mAsItem.has(entity)) return@addAction
                val item = sokol.entityHoster.hostItem(removedEntity.toBlueprint())
                player.give(item)
            }
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val position = mPosition.get(entity)
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
                        obj !is TrackedPhysicsObject || obj.id != collider?.bodyData?.bodyId
                    }

                holdState.transform = if (result == null) {
                    Transform(
                        (from + direction * settings.holdDistance).position(),
                        from.rotation()
                    )
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
                            Euler3(z = (if (dir.y > 0.0) -yaw else yaw) + 2* PI).quaternion(EulerOrder.XYZ)
                    } else {
                        val v1 = Vector3.Up.cross(dir).normalized
                        val v2 = dir.cross(v1).normalized
                        quaternionOfAxes(v1, v2, dir)
                    }

                    Transform(hitPos, rotation)
                } + settings.placeTransform
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

        holdable.holdState = null
    }
}
