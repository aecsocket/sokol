package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent
import net.kyori.adventure.text.Component

data class DisplayName(val i18nKey: String) : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<DisplayName>()
            ctx.system { DisplayNameTarget }
        }
    }

    override val componentType get() = DisplayName::class

    fun nameFor(i18n: I18N<Component>) = i18n.safeOne(i18nKey)
}

object DisplayNameTarget : SokolSystem
