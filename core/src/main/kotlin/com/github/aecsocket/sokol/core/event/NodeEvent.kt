package com.github.aecsocket.sokol.core.event

import com.github.aecsocket.alexandria.core.Input

interface NodeEvent {
    class OnTick(
    ) : NodeEvent

    data class OnInput(
        val input: Input
    ) : NodeEvent
}
