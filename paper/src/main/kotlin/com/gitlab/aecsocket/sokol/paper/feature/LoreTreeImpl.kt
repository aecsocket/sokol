package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.core.ColumnAlign
import com.gitlab.aecsocket.alexandria.core.extension.repeat
import com.gitlab.aecsocket.alexandria.paper.tableOfComponents
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.feature.LoreTreeFeature
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.paper.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.spongepowered.configurate.ConfigurationNode
import java.util.*

class LoreTreeImpl(
    private val plugin: Sokol
) : LoreTreeFeature.Type<PaperFeature.Profile>(), PaperFeature {
    override fun createProfile(node: ConfigurationNode) = Profile()

    inner class Profile : LoreTreeFeature.Profile<PaperFeature.Data>(), PaperFeature.Profile {
        override val type: LoreTreeImpl get() = this@LoreTreeImpl

        override fun createData() = Data()

        override fun createData(node: ConfigurationNode) = Data()

        override fun createData(tag: CompoundBinaryTag) = Data()

        inner class Data : LoreTreeFeature.Data<PaperFeature.State>(), PaperFeature.Data {
            override val type: LoreTreeImpl get() = this@LoreTreeImpl
            override val profile: Profile get() = this@Profile

            override fun createState() = State()

            override fun copy() = Data()
        }

        inner class State : LoreTreeFeature.State<
            PaperFeature.State, PaperFeature.Data, PaperFeatureContext,
        >(), PaperFeature.State {
            override val type: LoreTreeImpl get() = this@LoreTreeImpl
            override val profile: Profile get() = this@Profile

            override fun asData() = Data()

            override fun i18n(locale: Locale?, action: I18N<Component>.() -> List<Component>) =
                action(plugin.i18n.apply { withLocale(locale ?: this.locale) })

            override fun tableOf(
                rows: List<Pair<Int, Iterable<Component>>>,
                padding: Component,
                separator: Component
            ): List<Component> {
                return tableOfComponents(rows.map { it.second }, separator) { when (it) {
                    0 -> ColumnAlign.RIGHT
                    else -> ColumnAlign.LEFT
                } }.mapIndexed { idx, content ->
                    val (depth) = rows[idx]
                    text().append(padding.repeat(depth)).append(content).build()
                }
            }
        }
    }
}
