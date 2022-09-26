package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.paper.component.HostableByItem
import com.gitlab.aecsocket.sokol.paper.util.create
import com.gitlab.aecsocket.sokol.paper.util.spawnMarkerEntity
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

fun interface PersistentComponentFactory {
    fun create(): PersistentComponent
}

open class ItemBlueprint(
    private val sokol: Sokol,
    val factories: Map<Key, PersistentComponentFactory>,
) {
    fun createItem(): ItemStack {
        val hostable = factories[HostableByItem.Key]?.create() as? HostableByItem
            ?: throw IllegalStateException("ItemBlueprint must have component ${HostableByItem.Key}")
        val stack = hostable.backing.descriptor.create()
        return stack.withMeta { meta ->
            val entity = sokol.engine.createEntity()
            factories.forEach { (_, factory) -> entity.addComponent(factory.create()) }
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
    factories: Map<Key, PersistentComponentFactory>,
) : ItemBlueprint(sokol, factories), Keyed

open class EntityBlueprint(
    private val sokol: Sokol,
    val factories: Map<Key, PersistentComponentFactory>,
) {
    fun spawnEntity(location: Location): Entity {
        return spawnMarkerEntity(location) { stand ->
            val entity = sokol.engine.createEntity()
            factories.forEach { (_, factory) -> entity.addComponent(factory.create()) }
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
    factories: Map<Key, PersistentComponentFactory>,
) : EntityBlueprint(sokol, factories), Keyed
