package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.sokol.core.SokolComponent

interface IsValidSupplier : SokolComponent {
    override val componentType get() = IsValidSupplier::class.java

    val valid: () -> Boolean
}
