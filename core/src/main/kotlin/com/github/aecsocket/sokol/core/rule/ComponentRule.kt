package com.github.aecsocket.sokol.core.rule

import com.github.aecsocket.sokol.core.DataNode
import com.github.aecsocket.sokol.core.WalkResult
import java.util.Collections

data class HasTagsRule(
    val keys: Set<String>
) : Rule {
    override fun applies(target: DataNode) =
        !Collections.disjoint(keys, target.component.tags)
}

data class HasFeaturesRule(
    val keys: Set<String>
) : Rule {
    override fun applies(target: DataNode) =
        !Collections.disjoint(keys, target.features.keys)
}

object IsCompleteRule : Rule {
    override fun applies(target: DataNode): Boolean {
        return target.walkDataNodes { _, child ->
            if (child.component.slots.all { (key, slot) -> !slot.required || child.has(key) }) WalkResult.CONTINUE
            else WalkResult.STOP_ALL
        }
    }
}
