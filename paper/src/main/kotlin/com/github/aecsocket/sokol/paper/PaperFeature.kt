package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.FeatureContext

interface PaperFeature : Feature<PaperDataNode, PaperFeature.Profile> {
    interface Profile : Feature.Profile<Data>

    interface Data : Feature.Data<State> {
        fun copy(): Data
    }

    interface State : Feature.State<State, Data, PaperFeatureContext>
}

interface PaperFeatureContext : FeatureContext<PaperTreeState, PaperNodeHost, PaperDataNode>
