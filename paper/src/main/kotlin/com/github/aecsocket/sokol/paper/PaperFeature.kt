package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.Feature
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer

interface PaperFeature : Feature<PaperDataNode, PaperFeature.Profile> {
    interface Profile : Feature.Profile<Data> {
        fun deserialize(pdc: PersistentDataContainer): Data
    }

    interface Data : Feature.Data<State> {
        fun serialize(ctx: PersistentDataAdapterContext): PersistentDataContainer

        fun copy(): Data
    }

    interface State : Feature.State<State, PaperTreeState>
}
