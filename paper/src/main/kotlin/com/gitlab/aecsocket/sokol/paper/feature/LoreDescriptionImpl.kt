package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.sokol.core.feature.LoreDescriptionFeature
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode

class LoreDescriptionImpl(
    val plugin: Sokol
) : LoreDescriptionFeature.Type<PaperFeature.Profile>(), PaperFeature {
    override fun createProfile(node: ConfigurationNode) = Profile()

    inner class Profile : LoreDescriptionFeature.Profile<PaperFeature.Data>(), PaperFeature.Profile {
        override val type: LoreDescriptionImpl get() = this@LoreDescriptionImpl

        override fun createData() = Data()

        override fun createData(node: ConfigurationNode) = Data()

        override fun createData(tag: CompoundBinaryTag) = Data()

        inner class Data : LoreDescriptionFeature.Data<PaperFeature.State>(), PaperFeature.Data {
            override val type: LoreDescriptionImpl get() = this@LoreDescriptionImpl
            override val profile: Profile get() = this@Profile

            override fun createState() = State()

            override fun copy() = Data()
        }

        inner class State : LoreDescriptionFeature.State<
            PaperFeature.State, PaperFeature.Data, PaperFeatureContext,
        >(), PaperFeature.State {
            override val type: LoreDescriptionImpl get() = this@LoreDescriptionImpl
            override val profile: Profile get() = this@Profile
            override val plugin: Sokol get() = this@LoreDescriptionImpl.plugin

            override fun asData() = Data()
        }
    }
}
