package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.*
import com.gitlab.aecsocket.sokol.paper.util.spawnMarkerEntity
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class EntityHoster internal constructor(
    private val sokol: Sokol
) {
    private lateinit var mAsMob: ComponentMapper<HostableByMob>
    private lateinit var mAsItem: ComponentMapper<HostableByItem>
    private lateinit var mMob: ComponentMapper<HostedByMob>
    private lateinit var mItem: ComponentMapper<HostedByItem>
    private lateinit var mRotation: ComponentMapper<Rotation>

    internal fun enable() {
        mAsMob = sokol.engine.componentMapper()
        mAsItem = sokol.engine.componentMapper()
        mMob = sokol.engine.componentMapper()
        mItem = sokol.engine.componentMapper()
        mRotation = sokol.engine.componentMapper()
    }

    fun canHost(blueprint: EntityBlueprint, type: SokolObjectType): Boolean {
        return when (type) {
            SokolObjectType.Mob -> mAsMob.has(blueprint)
            SokolObjectType.Item -> mAsItem.has(blueprint)
            else -> false
        }
    }

    private fun hostError(key: Key, type: String): Nothing {
        throw IllegalArgumentException("Blueprint must have component $key to be hosted as $type")
    }

    fun hostMobOr(blueprint: EntityBlueprint, location: Location): Mob? {
        if (!mAsMob.has(blueprint))
            return null

        return spawnMarkerEntity(location) { mob ->
            mMob.set(blueprint, hostedByMob(mob))

            val entity = sokol.engine.buildEntity(blueprint)
            entity.call(SokolEvent.Add)
            sokol.mobsAdded.add(mob.entityId)

            sokol.persistence.writeEntityTagTo(entity, mob.persistentDataContainer)
        }
    }

    fun hostMob(blueprint: EntityBlueprint, location: Location) = hostMobOr(blueprint, location)
        ?: hostError(HostableByMob.key, "mob")

    fun hostMobOr(blueprint: EntityBlueprint, world: World, transform: Transform): Entity? {
        val location = transform.translation.location(world)
        mRotation.set(blueprint, Rotation(transform.rotation))
        return hostMobOr(blueprint, location)
    }

    fun hostMob(blueprint: EntityBlueprint, world: World, transform: Transform) = hostMobOr(blueprint, world, transform)
        ?: hostError(HostableByMob.key, "mob")

    fun hostItemOr(blueprint: EntityBlueprint): ItemStack? {
        val hostable = mAsItem.getOr(blueprint) ?: return null
        val item = hostable.profile.descriptor.create()

        return item.withMeta { meta ->
            mItem.set(blueprint, hostedByItem(item, meta))

            val entity = sokol.engine.buildEntity(blueprint)
            entity.call(ItemEvent.CreateForm(item, meta))
            entity.call(ItemEvent.Create)

            sokol.persistence.writeEntityTagTo(entity, meta.persistentDataContainer)
        }
    }

    fun hostItem(blueprint: EntityBlueprint) = hostItemOr(blueprint)
        ?: hostError(HostableByItem.Key, "item")

    fun createItemFormOr(entity: SokolEntity): ItemStack? {
        val hostable = mAsItem.getOr(entity) ?: return null
        val item = hostable.profile.descriptor.create()

        return item.withMeta { meta ->
            entity.call(ItemEvent.CreateForm(item, meta))
        }
    }

    fun createItemForm(entity: SokolEntity) = createItemFormOr(entity)
        ?: hostError(HostableByItem.Key, "item")
}
