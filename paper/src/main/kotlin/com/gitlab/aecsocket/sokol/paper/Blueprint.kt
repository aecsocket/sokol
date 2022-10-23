package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.Archetype
import com.gitlab.aecsocket.sokol.core.SokolEntityAccess
import com.gitlab.aecsocket.sokol.paper.component.HostableByItem
import com.gitlab.aecsocket.sokol.paper.util.spawnMarkerEntity
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

fun interface PersistentComponentFactory {
    fun create(): PersistentComponent
}

open class PersistentBlueprint(
    protected val sokol: Sokol,
    val factories: Map<String, PersistentComponentFactory>
) {
    private val archetype = sokol.engine.createArchetype(factories.map { (_, component) -> component.create()::class.java })

    fun archetype() = Archetype(archetype)

    fun factoryFor(key: Key) = factories[key.asString()]

    fun createEntity(): SokolEntityAccess {
        return sokol.engine.createEntity(archetype).also {
            factories.forEach { (_, factory) -> it.addComponent(factory.create()) }
        }
    }
}

open class ItemBlueprint(
    sokol: Sokol,
    factories: Map<String, PersistentComponentFactory>,
) : PersistentBlueprint(sokol, factories) {
    fun createItem(): ItemStack {
        val hostable = factoryFor(HostableByItem.Key)?.create() as? HostableByItem
            ?: throw IllegalStateException("ItemBlueprint must have component ${HostableByItem.Key}")
        val stack = hostable.backing.descriptor.create()
        return stack.withMeta { meta ->
            val entity = createEntity()
            sokol.entityResolver.populate(entity, stack, meta)
            entity.call(ItemEvent.Host)

            val tag = sokol.persistence.newTag()
            sokol.persistence.writeEntity(entity, tag)
            sokol.persistence.writeTagTo(tag, sokol.persistence.entityKey, meta.persistentDataContainer)
        }
    }
}

class KeyedItemBlueprint(
    sokol: Sokol,
    override val id: String,
    factories: Map<String, PersistentComponentFactory>,
) : ItemBlueprint(sokol, factories), Keyed

open class EntityBlueprint(
    sokol: Sokol,
    factories: Map<String, PersistentComponentFactory>,
) : PersistentBlueprint(sokol, factories) {
    fun spawnEntity(location: Location): Entity {
        return spawnMarkerEntity(location) { stand ->
            val entity = createEntity()
            sokol.entityResolver.populate(entity, stand)
            entity.call(MobEvent.Host)

            val tag = sokol.persistence.newTag()
            sokol.persistence.writeEntity(entity, tag)
            sokol.persistence.writeTagTo(tag, sokol.persistence.entityKey, stand.persistentDataContainer)
        }
    }
}

class KeyedEntityBlueprint(
    sokol: Sokol,
    override val id: String,
    factories: Map<String, PersistentComponentFactory>,
) : EntityBlueprint(sokol, factories), Keyed
