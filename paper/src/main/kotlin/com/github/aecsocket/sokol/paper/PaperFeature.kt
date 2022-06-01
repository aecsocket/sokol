package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.Feature
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer

interface PaperFeature : Feature<PaperDataNode, PaperFeature.Profile> {
    interface Profile : Feature.Profile<Data>

    interface Data : Feature.Data<State> {
        fun copy(): Data
    }

    interface State : Feature.State<State, PaperDataNode, PaperNodeHost, PaperTreeState>
}
