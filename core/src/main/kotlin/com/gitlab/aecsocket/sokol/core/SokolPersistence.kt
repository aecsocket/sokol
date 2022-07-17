package com.gitlab.aecsocket.sokol.core

interface SokolPersistence<N : DataNode> {
    fun stringToNode(string: String): N

    fun nodeToString(node: N): String
}
