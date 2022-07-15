package com.github.aecsocket.sokol.core.event

import com.github.aecsocket.alexandria.core.Input

interface NodeEvent {
    interface OnTick : NodeEvent

    interface OnInput : NodeEvent {
        val input: Input
    }
}
