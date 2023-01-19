package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.paper.component.ContainerMap
import com.gitlab.aecsocket.sokol.paper.component.ItemLoreStats
import com.gitlab.aecsocket.sokol.paper.component.Stats

class SokolComponents internal constructor(sokol: Sokol) {
    val stats = Stats.Type()
    val itemLoreStats = ItemLoreStats.Type()
}
