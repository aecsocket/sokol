package com.gitlab.aecsocket.sokol.core

import net.kyori.adventure.key.Key

fun interface EntityProvider {
    fun entities(): Iterable<SokolEntity>
}

class Composite(override val engine: SokolEngine) : SokolComponent, EntitySpace {
    object Attach : SokolEvent

    object Detach : SokolEvent

    override val componentType get() = Composite::class

    private val _entityProviders = HashMap<Key, EntityProvider>()

    fun entityProvider(key: Key, provider: EntityProvider) {
        if (_entityProviders.contains(key))
            throw IllegalArgumentException("Duplicate entity provider $key")
        _entityProviders[key] = provider
    }

    override fun entities() = _entityProviders.values.flatMap { it.entities() }
}

fun ComponentMapper<Composite>.forward(entity: SokolEntity, event: SokolEvent) {
    getOr(entity)?.call(event)
}

// more efficient (theoretically?) since we get all the entities in the tree first
// then run only ONE #call on that entity space
fun ComponentMapper<Composite>.forwardAll(entity: SokolEntity, event: SokolEvent) {
    entitySpaceOf(entity.engine, all(entity)).call(event)
}

data class IsChild(
    val parent: SokolEntity,
    val root: SokolEntity,
    val detach: () -> Unit
) : SokolComponent {
    override val componentType get() = IsChild::class
}

fun ComponentMapper<IsChild>.root(entity: SokolEntity) = getOr(entity)?.root ?: entity

fun ComponentMapper<Composite>.all(entity: SokolEntity): Collection<SokolEntity> {
    val entities = ArrayList<SokolEntity>()

    fun add(entity: SokolEntity) {
        entities.add(entity)
        val composite = getOr(entity) ?: return
        composite.entities().forEach { child ->
            add(child)
        }
    }

    add(entity)
    return entities
}

fun ComponentMapper<IsChild>.firstParent(entity: SokolEntity, predicate: (SokolEntity) -> Boolean): SokolEntity? {
    var current = getOr(entity)?.parent ?: return null
    while (!predicate(current)) current = getOr(current)?.parent ?: return null
    return current
}

class CompositeMutator(ids: ComponentIdAccess) {
    private val mIsChild = ids.mapper<IsChild>()

    fun attach(parent: SokolEntity, child: SokolEntity, detach: () -> Unit) {
        if (mIsChild.has(child))
            throw IllegalStateException("Entity already has IsChild component")
        mIsChild.set(child, IsChild(parent, mIsChild.root(parent), detach))
        child.call(Composite.Attach)
    }

    fun detach(child: SokolEntity) {
        mIsChild.getOr(child)?.let {
            it.detach()
            mIsChild.remove(child)
            child.call(Composite.Detach)
        }
    }
}
