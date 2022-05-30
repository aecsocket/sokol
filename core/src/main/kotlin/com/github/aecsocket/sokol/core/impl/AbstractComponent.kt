package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.NodeComponent
import com.github.aecsocket.sokol.core.Slot

abstract class AbstractComponent<
    C : AbstractComponent<C, F, S>,
    F : Feature.Profile<*>,
    S : Slot
>(
    override val id: String,
    override val features: Map<String, F>,
    override val slots: Map<String, S>
) : NodeComponent.Scoped<C, F, S>
