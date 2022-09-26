package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.SokolComponent

interface IsValidSupplier : SokolComponent {
    override val componentType get() = IsValidSupplier::class.java

    val valid: () -> Boolean
}
