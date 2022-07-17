package com.gitlab.aecsocket.sokol.core.stat

import com.gitlab.aecsocket.sokol.core.rule.Rule


data class ApplicableStats(
    val stats: StatMap,
    val priority: Int = 0,
    val reversed: Boolean = false,
    val rule: Rule = Rule.True,
)
