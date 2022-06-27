package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.sokol.core.feature.ItemRepresentationFeature
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.paper.PaperFeatureContext
import org.spongepowered.configurate.ConfigurationNode

class PaperItemRepresentation : ItemRepresentationFeature<PaperItemRepresentation.Profile>() {
    override fun createProfile(node: ConfigurationNode) = Profile()

    inner class Profile : ItemRepresentationFeature<Profile>.Profile<Profile.Data>() {
        override fun createData() = Data()

        override fun createData(node: ConfigurationNode) = Data()

        override fun createData(tag: CompoundBinaryTag) = Data()

        inner class Data : ItemRepresentationFeature<Profile>.Profile<Data>.Data<State>() {
            override fun createState() = State()
        }

        inner class State : ItemRepresentationFeature<Profile>.Profile<Data>.State<State, Data, PaperFeatureContext>() {
            override fun asData() = Data()
        }
    }
}
