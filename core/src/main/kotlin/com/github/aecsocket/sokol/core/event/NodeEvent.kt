package com.github.aecsocket.sokol.core.event

import com.github.aecsocket.sokol.core.TreeState

interface NodeEvent {
    val state: TreeState<*, *, *> // todo
}

data class TestEvent(
    override val state: TreeState<*, *, *>,
    val data: Int
) : NodeEvent
