package com.gitlab.aecsocket.sokol.core

interface SokolEvent {
    object Add : SokolEvent

    object Update : SokolEvent

    object Remove : SokolEvent
}
