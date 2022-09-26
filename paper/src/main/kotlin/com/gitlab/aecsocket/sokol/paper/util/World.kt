package com.gitlab.aecsocket.sokol.paper.util

import com.gitlab.aecsocket.craftbullet.paper.hasCollision
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent

fun spawnMarkerEntity(location: Location, consumer: (ArmorStand) -> Unit = {}): Entity {
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
        consumer(this)
    } }
}
