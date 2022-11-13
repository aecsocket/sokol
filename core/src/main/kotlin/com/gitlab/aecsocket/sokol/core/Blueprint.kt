package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed

fun interface ComponentFactory<C : SokolComponent> {
    data class Context(val entity: SokolEntity, val space: SokolSpaceAccess)

    fun create(ctx: Context): C
}

open class EntityBlueprint(
    protected val flags: Int,
    protected val components: List<Pair<ComponentMapper<*>, ComponentFactory<*>>>
) {
    open fun <C : SokolComponent> with(mapper: ComponentMapper<C>, factory: ComponentFactory<C>) =
        EntityBlueprint(flags, components + (mapper to factory))

    fun createIn(space: SokolSpaceAccess): SokolEntity {
        val entity = space.createEntity(flags)

        fun <C : SokolComponent> set(mapper: ComponentMapper<*>, factory: ComponentFactory<C>) {
            val component = factory.create(ComponentFactory.Context(entity, space))
            @Suppress("UNCHECKED_CAST")
            (mapper as ComponentMapper<C>).set(entity, component)
        }

        components.forEach { (mapper, factory) -> set(mapper, factory) }
        return entity
    }
}

class KeyedEntityBlueprint(
    override val id: String,
    flags: Int,
    components: List<Pair<ComponentMapper<*>, ComponentFactory<*>>>
) : EntityBlueprint(flags, components), Keyed {
    override fun <C : SokolComponent> with(mapper: ComponentMapper<C>, factory: ComponentFactory<C>) =
        KeyedEntityBlueprint(id, flags, components + (mapper to factory))
}

fun Iterable<EntityBlueprint>.createAllIn(space: SokolSpaceAccess) {
    forEach { it.createIn(space) }
}
