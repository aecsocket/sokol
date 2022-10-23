package com.gitlab.aecsocket.sokol.core.extension

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
import com.jme3.bullet.collision.shapes.PlaneCollisionShape
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.math.Plane

private val EMPTY_SHAPE = com.jme3.bullet.collision.shapes.EmptyShape(false)

fun collisionOf(shape: Shape) = when (shape) {
    is EmptyShape -> EMPTY_SHAPE
    is PlaneShape -> PlaneCollisionShape(Plane(shape.normal.bullet(), 0f))
    is BoxShape -> BoxCollisionShape(shape.halfExtent.bullet())
    is SphereShape -> SphereCollisionShape(shape.radius.toFloat())
}

fun collisionOf(bodies: Collection<Body>) = CompoundCollisionShape(bodies.size).apply {
    bodies.forEach { body ->
        addChildShape(collisionOf(body.shape), body.transform.bullet())
    }
}
