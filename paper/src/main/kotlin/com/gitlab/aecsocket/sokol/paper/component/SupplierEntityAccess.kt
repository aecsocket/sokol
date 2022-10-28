package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

// NB: this might seem like a weird component
// but sometimes you get passed a "staling" entity reference, which can't persist anything
// (e.g. SokolPhysicsObject.entity)
// this accessor allows you to write to a non-stale entity
interface SupplierEntityAccess : SokolComponent {
    override val componentType get() = SupplierEntityAccess::class

    fun useEntity(consumer: (SokolEntity) -> Unit)
}

object SupplierEntityAccessTarget : SokolSystem

@All(SupplierEntityAccess::class, Composite::class)
@Before(SupplierEntityAccessTarget::class)
class SupplierEntityAccessBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mSupplierEntityAccess = mappers.componentMapper<SupplierEntityAccess>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val supplierEntityAccess = mSupplierEntityAccess.get(entity)

        mComposite.forEachChild(entity) { (_, child) ->
            mSupplierEntityAccess.set(child, supplierEntityAccess)
            child.call(Compose)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose)
    }

    object Compose : SokolEvent
}
