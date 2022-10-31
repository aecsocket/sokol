package com.gitlab.aecsocket.sokol.paper.util

import com.gitlab.aecsocket.sokol.paper.component.*
import com.jme3.bullet.collision.PhysicsRayTestResult

fun colliderHitPath(collider: Collider?, childIdx: Int): CompositePath {
    return if (childIdx == -1) emptyCompositePath()
    else {
        collider?.bodyData?.compositeMap?.let { compositeMap ->
            if (compositeMap.isEmpty() || childIdx >= compositeMap.size) emptyCompositePath()
            else compositeMap[childIdx]
        } ?: emptyCompositePath()
    }
}

fun colliderHitPath(collider: Collider?, rayTestResult: PhysicsRayTestResult) =
    colliderHitPath(collider, rayTestResult.triangleIndex())

