package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

interface SupplierIsValid : SokolComponent {
    override val componentType get() = SupplierIsValid::class

    val valid: () -> Boolean
}

object SupplierIsValidTarget : SokolSystem

@All(SupplierIsValid::class, Composite::class)
@Before(SupplierIsValidTarget::class)
class SupplierIsValidBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mSupplierIsValid = mappers.componentMapper<SupplierIsValid>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val supplierIsValid = mSupplierIsValid.get(entity)

        mComposite.forEachChild(entity) { (_, child) ->
            mSupplierIsValid.set(child, supplierIsValid)
            child.call(Compose)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose)
    }

    object Compose : SokolEvent
}
