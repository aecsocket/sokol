package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

interface SupplierEntityAccess : SokolComponent {
    override val componentType get() = SupplierEntityAccess::class

    fun useEntity(consumer: (SokolEntity) -> Unit)
}
