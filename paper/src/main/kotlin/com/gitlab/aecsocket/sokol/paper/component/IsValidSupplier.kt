package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

interface IsValidSupplier : SokolComponent {
    override val componentType get() = IsValidSupplier::class

    val valid: () -> Boolean
}

@All(IsValidSupplier::class, Composite::class)
class IsValidSupplierComposeSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mIsValidSupplier = mappers.componentMapper<IsValidSupplier>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val isValidSupplier = mIsValidSupplier.map(entity)

        mComposite.forEachChild(entity) { (_, child) ->
            child.components.set(isValidSupplier)
            child.call(Compose)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose)
    }

    object Compose : SokolEvent
}
