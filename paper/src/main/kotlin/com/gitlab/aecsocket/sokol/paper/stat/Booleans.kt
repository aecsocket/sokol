package com.gitlab.aecsocket.sokol.paper.stat

import com.gitlab.aecsocket.alexandria.core.TableCell
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.paper.component.Stat
import com.gitlab.aecsocket.sokol.paper.component.StatFormatter
import com.gitlab.aecsocket.sokol.paper.component.StatNode
import com.gitlab.aecsocket.sokol.paper.component.StatValue
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class BooleanStat(override val key: Key) : Stat<Boolean> {
    data class Set(val value: Boolean) : StatNode.First<Boolean> {
        override fun first() = value
        override fun with(last: Boolean) = value
    }

    override fun createNode(node: ConfigurationNode) = Set(node.force())
}

@ConfigSerializable
data class BooleanStatFormatter(
    @Required val key: String
) : StatFormatter<Boolean> {
    override fun format(i18n: I18N<Component>, value: StatValue<Boolean>): Iterable<TableCell<Component>> {
        val bool = value.compute()
        val text = i18n.safe("$key.$bool")
        return listOf(text)
    }
}
