package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.craftbullet.paper.hasCollision
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent

open class EntityBlueprint(private val sokol: Sokol, val backing: SokolBlueprint) {
    init {
        if (!backing.containsType<HostableByEntity>())
            throw IllegalArgumentException("Backing blueprint must have component of type ${HostableByEntity::class}")
    }

    fun spawnEntity(location: Location): Entity {
        return location.world.spawnEntity(
            location, EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.CUSTOM
        ) { mob -> (mob as ArmorStand).apply {
            isVisible = false
            hasCollision = false
            isMarker = true
            isSilent = true
            setAI(false)
            setGravity(false)
            setCanTick(false)

            val entity = backing.create(sokol.engine)
            sokol.entityResolver.populate(entity, mob)
            entity.call(EntityEvent.Host)

            val tag = sokol.persistence.newTag()
            sokol.persistence.writeEntity(entity, tag)
            sokol.persistence.writeTagTo(tag, sokol.persistence.entityKey, mob.persistentDataContainer)
        } }
    }
}

class KeyedEntityBlueprint(
    sokol: Sokol,
    override val id: String,
    backing: SokolBlueprint
) : EntityBlueprint(sokol, backing), Keyed
