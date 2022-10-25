package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject

interface SokolEvent {
    interface Cancellable : SokolEvent {
        var cancelled: Boolean
    }

    object Populate : SokolEvent

    object Add : SokolEvent

    object Update : SokolEvent

    object Remove : SokolEvent

    object Reload : SokolEvent
}

fun SokolEvent.Cancellable.cancel() {
    cancelled = true
}
