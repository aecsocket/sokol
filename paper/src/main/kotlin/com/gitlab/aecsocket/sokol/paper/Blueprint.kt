package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.SokolEntityBuilder
import com.gitlab.aecsocket.sokol.core.SokolEvent
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
    fun factoryFor(key: Key) = factories[key.asString()]

    fun builder(): SokolEntityBuilder {
        return sokol.engine.entityBuilder().apply {
            factories.forEach { (_, factory) ->
                addComponent(factory.create())
            }
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
            val entity = builder().apply {
                sokol.entityResolver.populate(SokolObjectType.Item, this, ItemData({ stack }, { meta }))
            }.build()
            entity.call(SokolEvent.Add)

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

open class MobBlueprint(
    sokol: Sokol,
    factories: Map<String, PersistentComponentFactory>,
) : PersistentBlueprint(sokol, factories) {
    fun spawnMob(location: Location): Entity {
        return spawnMarkerEntity(location) { stand ->
            val entity = builder().apply {
                sokol.entityResolver.populate(SokolObjectType.Mob, this, stand)
            }.build()
            entity.call(SokolEvent.Add)
            sokol.mobsAdded.add(stand.entityId)

            val tag = sokol.persistence.newTag()
            sokol.persistence.writeEntity(entity, tag)
            sokol.persistence.writeTagTo(tag, sokol.persistence.entityKey, stand.persistentDataContainer)
        }
    }
}

class KeyedMobBlueprint(
    sokol: Sokol,
    override val id: String,
    factories: Map<String, PersistentComponentFactory>,
) : MobBlueprint(sokol, factories), Keyed
