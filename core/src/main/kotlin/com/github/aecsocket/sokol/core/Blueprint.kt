package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.keyed.Keyed

interface Blueprint : Keyed {
    val node: DataNode
}