package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.sokol.core.feature.ItemHostFeature
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.paper.PaperFeature
import com.gitlab.aecsocket.sokol.paper.PaperFeatureContext
import com.gitlab.aecsocket.sokol.paper.Sokol
import org.spongepowered.configurate.ConfigurationNode

class PaperItemHost(
    private val plugin: Sokol
) : ItemHostFeature.Type<PaperFeature.Profile>(), PaperFeature {
    override fun createProfile(node: ConfigurationNode) = Profile()

    inner class Profile : ItemHostFeature.Profile<PaperFeature.Data>(), PaperFeature.Profile {
        override val type: PaperItemHost get() = this@PaperItemHost

        override fun createData() = Data()

        override fun createData(node: ConfigurationNode) = Data()

        override fun createData(tag: CompoundBinaryTag) = Data()

        inner class Data : ItemHostFeature.Data<PaperFeature.State>(), PaperFeature.Data {
            override val type: PaperItemHost get() = this@PaperItemHost
            override val profile: Profile get() = this@Profile

            override fun createState() = State()

            override fun copy() = Data()
        }

        inner class State : ItemHostFeature.State<PaperFeature.State, PaperFeature.Data, PaperFeatureContext>(), PaperFeature.State {
            override val type: PaperItemHost get() = this@PaperItemHost
            override val profile: Profile get() = this@Profile

            override fun asData() = Data()
        }
    }
}
