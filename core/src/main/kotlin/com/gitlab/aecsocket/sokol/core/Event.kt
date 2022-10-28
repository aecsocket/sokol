package com.gitlab.aecsocket.sokol.core

interface SokolEvent {
    interface Cancellable : SokolEvent {
        var cancelled: Boolean
    }

    data class Populate(
        val blueprint: EntityBlueprint
    ) : SokolEvent

    object Add : SokolEvent

    object Update : SokolEvent

    object Remove : SokolEvent

    object Reload : SokolEvent
}

fun SokolEvent.Cancellable.cancel() {
    cancelled = true
}
