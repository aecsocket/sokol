package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.Slot
import com.github.aecsocket.sokol.core.impl.AbstractComponent
import com.github.aecsocket.sokol.core.rule.Rule

class PaperComponent(
    id: String,
    features: Map<String, PaperFeature.Profile>,
    slots: Map<String, PaperSlot>
) : AbstractComponent<PaperComponent, PaperFeature.Profile, PaperSlot>(id, features, slots)

data class PaperSlot(
    override val key: String,
    override val tags: Set<String>,
    val required: Boolean,
    val modifiable: Boolean,
    val compatible: Rule
) : Slot
