package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.craftbullet.core.ContactContext
import com.gitlab.aecsocket.craftbullet.core.DoesContactContext
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.ColliderSystem
import com.gitlab.aecsocket.sokol.paper.component.SokolPhysicsObject
import com.jme3.bullet.collision.PhysicsCollisionObject

class EntityPhysics internal constructor(private val sokol: Sokol) {
    private lateinit var mIsChild: ComponentMapper<IsChild>

    internal fun enable() {
        sokol.engine.apply {
            mIsChild = mapper()
        }

        CraftBulletAPI.onDoesContact(::onDoesContact)
        CraftBulletAPI.onContact(::onContact)
        CraftBulletAPI.onPostStep(::onPostStep)
    }

    private fun onDoesContact(ctx: DoesContactContext) {
        val (bodyA, bodyB) = ctx
        if (bodyA !is SokolPhysicsObject || bodyB !is SokolPhysicsObject) return

        if (mIsChild.root(bodyA.entity) === mIsChild.root(bodyB.entity)) {
            ctx.doesContact = false
        }
    }

    private fun onContact(ctx: ContactContext) {
        val (bodyA, bodyB, point) = ctx

        fun callEvent(thisBody: PhysicsCollisionObject, otherBody: PhysicsCollisionObject) {
            if (thisBody !is SokolPhysicsObject) return
            sokol.useSpaceOf(thisBody.entity) { space ->
                space.call(ColliderSystem.Contact(thisBody, otherBody, point))
            }
        }

        callEvent(bodyA, bodyB)
        callEvent(bodyB, bodyA)
    }

    private fun onPostStep() {
        sokol.players.postPhysicsStep()
    }
}
