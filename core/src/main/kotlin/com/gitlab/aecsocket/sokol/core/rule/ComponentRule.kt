package com.gitlab.aecsocket.sokol.core.rule

import com.gitlab.aecsocket.sokol.core.DataNode
import com.gitlab.aecsocket.sokol.core.WalkResult
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
        return target.walkDataNodes { child, _ ->
            if (child.component.slots.all { (key, slot) -> !slot.required || child.has(key) }) WalkResult.CONTINUE
            else WalkResult.STOP_ALL
        }
    }
}
