package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import net.kyori.adventure.text.Component

data class DisplayName(val i18nKey: String) : SokolComponent {
    override val componentType get() = DisplayName::class

    fun nameFor(i18n: I18N<Component>) = i18n.safeOne(i18nKey)
}

object DisplayNameTarget : SokolSystem
