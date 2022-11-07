package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

interface SupplierEntityAccess : SokolComponent {
    override val componentType get() = SupplierEntityAccess::class

    fun useEntity(builder: (EntityBlueprint) -> Unit = {}, consumer: (SokolEntity) -> Unit)
}

fun ComponentMapper<SupplierEntityAccess>.apply(
    entity: SokolEntity,
    builder: (EntityBlueprint) -> Unit = {},
    consumer: (SokolEntity) -> Unit
) {
    getOr(entity)?.useEntity(builder, consumer)
}
