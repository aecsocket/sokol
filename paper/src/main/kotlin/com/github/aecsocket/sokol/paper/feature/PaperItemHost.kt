package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.sokol.core.feature.ItemHostFeature
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.paper.PaperFeatureContext
import org.spongepowered.configurate.ConfigurationNode

class PaperItemHost : ItemHostFeature<PaperItemHost.Profile>() {
    override fun createProfile(node: ConfigurationNode) = Profile()

    inner class Profile : ItemHostFeature<Profile>.Profile<Profile.Data>() {
        override fun createData() = Data()

        override fun createData(node: ConfigurationNode) = Data()

        override fun createData(tag: CompoundBinaryTag) = Data()

        inner class Data : ItemHostFeature<Profile>.Profile<Data>.Data<State>() {
            override fun createState() = State()
        }

        inner class State : ItemHostFeature<Profile>.Profile<Data>.State<State, Data, PaperFeatureContext>() {
            override fun asData() = Data()
        }
    }
}
