package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.HostableByItem
import com.gitlab.aecsocket.sokol.paper.component.HostableByMob
import com.gitlab.aecsocket.sokol.paper.component.hostedByItem
import com.gitlab.aecsocket.sokol.paper.component.hostedByMob
import com.gitlab.aecsocket.sokol.paper.util.spawnMarkerEntity
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.inventory.ItemStack

class EntityHoster internal constructor(
    private val sokol: Sokol
) {
    private lateinit var mMob: ComponentMapper<HostableByMob>
    private lateinit var mItem: ComponentMapper<HostableByItem>

    internal fun enable() {
        mMob = sokol.engine.componentMapper()
        mItem = sokol.engine.componentMapper()
    }

    fun canHost(blueprint: EntityBlueprint, type: SokolObjectType): Boolean {
        return when (type) {
            SokolObjectType.Mob -> mMob.has(blueprint.components)
            SokolObjectType.Item -> mItem.has(blueprint.components)
            else -> false
        }
    }

    private fun hostError(key: Key, type: String): Nothing {
        throw IllegalArgumentException("Blueprint must have component $key to be hosted as $type")
    }

    fun hostMobOr(blueprint: EntityBlueprint, location: Location): Mob? {
        if (!mMob.has(blueprint.components))
            return null

        return spawnMarkerEntity(location) { mob ->
            blueprint.components.set(hostedByMob(mob))

            val entity = sokol.engine.buildEntity(blueprint)
            entity.call(SokolEvent.Add)
            sokol.mobsAdded.add(mob.entityId)

            sokol.persistence.writeEntityTagTo(entity, mob.persistentDataContainer)
        }
    }

    fun hostMob(blueprint: EntityBlueprint, location: Location) = hostMobOr(blueprint, location)
        ?: hostError(HostableByMob.key, "mob")

    fun hostItemOr(blueprint: EntityBlueprint): ItemStack? {
        val hostable = mItem.mapOr(blueprint.components) ?: return null
        val item = hostable.profile.descriptor.create()

        return item.withMeta { meta ->
            blueprint.components.set(hostedByItem(item, meta))

            val entity = sokol.engine.buildEntity(blueprint)
            entity.call(SokolEvent.Add)

            sokol.persistence.writeEntityTagTo(entity, meta.persistentDataContainer)
        }
    }

    fun hostItem(blueprint: EntityBlueprint) = hostItemOr(blueprint)
        ?: hostError(HostableByItem.Key, "item")
}
