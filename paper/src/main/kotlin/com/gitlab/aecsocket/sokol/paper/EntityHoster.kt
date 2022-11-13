package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.*
import com.gitlab.aecsocket.sokol.paper.util.spawnMarkerMob
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

class EntityHoster internal constructor(
    private val sokol: Sokol
) {
    private lateinit var mAsMob: ComponentMapper<AsMob>
    private lateinit var mAsItem: ComponentMapper<AsItem>
    private lateinit var mRotation: ComponentMapper<Rotation>

    internal fun enable() {
        mAsMob = sokol.engine.mapper()
        mAsItem = sokol.engine.mapper()
        mRotation = sokol.engine.mapper()
    }

    private fun hostError(key: Key, type: String): Nothing {
        throw IllegalArgumentException("Blueprint must have component $key to be hosted as $type")
    }

    fun hostMob(entity: SokolEntity, space: SokolSpace, location: Location): Entity {
        return spawnMarkerMob(location) { mob ->
            space.call(MobEvent.Create(mob))
            sokol.mobsAdded.add(mob.entityId)
            sokol.persistence.writeEntityTagTo(entity, mob.persistentDataContainer)
        }
    }

    fun hostMob(entity: SokolEntity, space: SokolSpace, world: World, transform: Transform): Entity {
        val location = transform.translation.location(world)
        mRotation.set(entity, Rotation(transform.rotation))
        return hostMob(entity, space, location)
    }

    fun hostItem(entity: SokolEntity, space: SokolSpace): ItemStack {
        val item = mAsItem.get(entity).profile.item.create()
        return item.withMeta { meta ->
            space.call(ItemEvent.CreateForm(item, meta))
            space.call(ItemEvent.Create(item, meta))
            sokol.persistence.writeEntityTagTo(entity, meta.persistentDataContainer)
        }
    }

    fun createItemForm(entity: SokolEntity, space: SokolSpace): ItemStack {
        val item = mAsItem.get(entity).profile.item.create()
        return item.withMeta { meta ->
            space.call(ItemEvent.CreateForm(item, meta))
        }
    }
}
