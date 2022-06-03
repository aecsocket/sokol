package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.glossa.core.I18N
import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.NodeComponent
import com.github.aecsocket.sokol.core.Slot
import net.kyori.adventure.text.Component

abstract class AbstractComponent<
    C : AbstractComponent<C, F, S>,
    F : Feature.Profile<*>,
    S : Slot
>(
    override val id: String,
    override val features: Map<String, F>,
    override val slots: Map<String, S>,
    override val tags: Set<String>
) : NodeComponent.Scoped<C, F, S> {
    override fun localize(i18n: I18N<Component>) =
        i18n.safe("component.$id")
}

open class SimpleSlot(
    override val key: String,
    override val tags: Set<String>
) : Slot {
    override fun localize(i18n: I18N<Component>) =
        i18n.safe("slot.$key")
}
