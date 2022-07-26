package com.gitlab.aecsocket.sokol.core.event

import com.gitlab.aecsocket.alexandria.core.Input

interface NodeEvent {
    interface OnTick : NodeEvent

    interface OnHosted : NodeEvent

    interface OnHostUpdate : NodeEvent

    interface OnInput : NodeEvent {
        val input: Input
    }

    interface OnItemClick : NodeEvent {
        val leftClick: Boolean
        val rightClick: Boolean
        val shiftClick: Boolean
    }

    interface OnClickAsCurrent : OnItemClick

    interface OnClickAsCursor : OnItemClick
}
