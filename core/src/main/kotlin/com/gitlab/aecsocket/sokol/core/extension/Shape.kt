package com.gitlab.aecsocket.sokol.core.extension

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.craftbullet.core.sp
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.collision.shapes.PlaneCollisionShape
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.math.Plane

private val emptyShape = com.jme3.bullet.collision.shapes.EmptyShape(false)

fun Shape.bullet(): CollisionShape = when (this) {
    is EmptyShape -> emptyShape
    is CompoundShape -> CompoundCollisionShape(children.size).apply {
        children.forEach { child ->
            addChildShape(child.shape.bullet(), child.transform.bullet().sp())
        }
    }
    is PlaneShape -> PlaneCollisionShape(Plane(normal.bullet().sp(), 0f))
    is BoxShape -> BoxCollisionShape(halfExtent.bullet().sp())
    is SphereShape -> SphereCollisionShape(radius.toFloat())
    is CylinderShape -> CylinderCollisionShape(radius.toFloat(), height.toFloat(), axis.ordinal)
}
