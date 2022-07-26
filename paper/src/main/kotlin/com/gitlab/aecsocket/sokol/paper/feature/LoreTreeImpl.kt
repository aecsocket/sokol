package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.core.*
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.sokol.core.feature.LoreTreeFeature
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.util.TableFormat
import com.gitlab.aecsocket.sokol.paper.*
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import kotlin.collections.ArrayList

private const val AT_TOP = "at_top"
private const val AT_BOTTOM = "at_bottom"
private const val TABLE_FORMAT = "table_format"

class LoreTreeImpl(
    val plugin: Sokol
) : LoreTreeFeature.Type<PaperFeature.Profile>(), PaperFeature {
    override fun createProfile(node: ConfigurationNode) = Profile(
        node.node(AT_TOP).get { ArrayList() },
        node.node(AT_BOTTOM).get { ArrayList() },
        node.node(TABLE_FORMAT).get { TableFormat(
            defaultedMapOf(mapOf(0 to TableAlign.END), TableAlign.START),
            emptyDefaultedMap(TableAlign.START),
        ) }.buildRenderer(plugin),
    )

    inner class Profile(
        atTop: List<String>,
        atBottom: List<String>,
        tableRenderer: ComponentTableRenderer,
    ) : LoreTreeFeature.Profile<PaperFeature.Data>(
        atTop, atBottom, tableRenderer,
    ), PaperFeature.Profile {
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
            override val plugin: Sokol get() = this@LoreTreeImpl.plugin

            override fun asData() = Data()

            override fun widthOf(value: Component) =
                AlexandriaAPI.widthOf(value)
        }
    }
}
