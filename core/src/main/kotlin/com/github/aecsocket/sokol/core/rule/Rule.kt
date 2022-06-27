package com.github.aecsocket.sokol.core.rule

import com.github.aecsocket.sokol.core.DataNode

interface Rule {
    fun applies(target: DataNode): Boolean

    object True : Rule {
        override fun applies(target: DataNode) = true
    }

    object False : Rule {
        override fun applies(target: DataNode) = false
    }

    companion object {
        fun of(value: Boolean) = if (value) True else False
    }
}
