package com.gitlab.aecsocket.sokol.core.rule

import com.gitlab.aecsocket.sokol.core.DataNode
import com.gitlab.aecsocket.sokol.core.NodePath

data class HasRule(
    val path: NodePath
) : Rule {
    override fun applies(target: DataNode) =
        target.has(path)
}

data class AsRule(
    val path: NodePath,
    val rule: Rule
) : Rule {
    override fun applies(target: DataNode) =
        target.node(path)?.let { rule.applies(it) } ?: false
}

data class AsRootRule(
    val path: NodePath,
    val rule: Rule
) : Rule {
    override fun applies(target: DataNode) =
        target.root().node(path)?.let { rule.applies(it) } ?: false
}

object IsRootRule : Rule {
    override fun applies(target: DataNode) =
        target.isRoot()
}
