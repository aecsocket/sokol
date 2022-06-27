package com.github.aecsocket.sokol.core.rule

import com.github.aecsocket.sokol.core.DataNode

data class NotRule(
    val rule: Rule
) : Rule {
    override fun applies(target: DataNode) =
        !rule.applies(target)
}

data class AllRule(
    val rules: List<Rule>
) : Rule {
    override fun applies(target: DataNode) = rules.all {
        it.applies(target)
    }
}

data class AnyRule(
    val rules: List<Rule>
) : Rule {
    override fun applies(target: DataNode) = rules.any {
        it.applies(target)
    }
}
