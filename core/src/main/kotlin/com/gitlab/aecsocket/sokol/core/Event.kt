package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject

interface SokolEvent {
    interface Cancellable : SokolEvent {
        var cancelled: Boolean
    }

    object Add : SokolEvent

    object Update : SokolEvent

    data class PhysicsUpdate(val obj: TrackedPhysicsObject) : SokolEvent

    object Remove : SokolEvent

    object Reload : SokolEvent
}

fun SokolEvent.Cancellable.cancel() {
    cancelled = true
}
