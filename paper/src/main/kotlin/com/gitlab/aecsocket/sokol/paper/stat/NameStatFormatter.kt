package com.gitlab.aecsocket.sokol.paper.stat

import com.gitlab.aecsocket.alexandria.core.TableCell
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.paper.component.StatFormatter
import com.gitlab.aecsocket.sokol.paper.component.StatValue
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

@ConfigSerializable
data class NameStatFormatter(
    @Required val key: String
) : StatFormatter<Any> {
    override fun format(i18n: I18N<Component>, value: StatValue<Any>): Iterable<TableCell<Component>> {
        val text = i18n.safe(key)
        return listOf(text)
    }
}
