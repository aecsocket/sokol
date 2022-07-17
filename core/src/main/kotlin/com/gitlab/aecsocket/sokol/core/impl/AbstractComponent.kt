package com.gitlab.aecsocket.sokol.core.impl

import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.DataNode
import com.gitlab.aecsocket.sokol.core.Feature
import com.gitlab.aecsocket.sokol.core.NodeComponent
import com.gitlab.aecsocket.sokol.core.Slot
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.ApplicableStats
import com.gitlab.aecsocket.sokol.core.stat.StatMap
import net.kyori.adventure.text.Component

abstract class AbstractComponent<
    C : AbstractComponent<C, F, S>,
    F : Feature.Profile<*>,
    S : Slot
>(
    override val id: String,
    override val tags: Set<String>,
    override val features: Map<String, F>,
    override val slots: Map<String, S>,
    override val stats: List<ApplicableStats>,
) : NodeComponent.Scoped<C, F, S> {
    override fun localize(i18n: I18N<Component>) =
        i18n.safe("component.$id")
}

open class SimpleSlot(
    override val key: String,
    override val tags: Set<String>,
    override val required: Boolean,
    val rule: Rule,
) : Slot {
    override fun localize(i18n: I18N<Component>) =
        i18n.safe("slot.$key")

    override fun compatible(node: DataNode) = rule.applies(node)
}
