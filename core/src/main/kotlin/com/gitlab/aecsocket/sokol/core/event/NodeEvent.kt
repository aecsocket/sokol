package com.gitlab.aecsocket.sokol.core.event

import com.gitlab.aecsocket.alexandria.core.Input

interface NodeEvent {
    interface OnTick : NodeEvent

    interface OnInput : NodeEvent {
        val input: Input
    }
}
