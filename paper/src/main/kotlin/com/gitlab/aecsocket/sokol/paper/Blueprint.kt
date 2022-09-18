package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.sokol.core.HostByEntityEvent
import com.gitlab.aecsocket.sokol.core.HostByItemEvent
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.force
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftAreaEffectCloud
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack

abstract class AbstractBlueprint(
    protected val sokol: Sokol,
    override val id: String,
    val components: List<SokolComponent>,
) : Keyed {
    fun createEntity() = sokol.engine.createEntity(components)
}

class ItemBlueprint(
    sokol: Sokol, id: String, components: List<SokolComponent>,
) : AbstractBlueprint(sokol, id, components) {
    fun createItem(): ItemStack {
        val entity = createEntity()
        val hostable = entity.force(HostableByItem)

        val stack = hostable.config.descriptor.create()
        val meta = stack.itemMeta
        entity.add(hostedByItem(stack, meta))
        sokol.engine.call(setOf(entity), HostByItemEvent)

        val tag = sokol.persistence.newTag()
        sokol.persistence.writeEntity(entity, tag)
        sokol.persistence.writeTagTo(tag, sokol.persistence.entityKey, meta.persistentDataContainer)

        stack.itemMeta = meta
        return stack
    }
}

class EntityBlueprint(
    sokol: Sokol, id: String, components: List<SokolComponent>,
) : AbstractBlueprint(sokol, id, components) {
    fun spawnEntity(location: Location): Entity {
        val entity = createEntity()
        entity.force(HostableByEntity)

        return location.world.spawnEntity(
            location, EntityType.AREA_EFFECT_CLOUD, CreatureSpawnEvent.SpawnReason.CUSTOM
        ) { mob ->
            (mob as CraftAreaEffectCloud).handle.apply {
                tickCount = Int.MIN_VALUE
                duration = -1
                waitTime = Int.MIN_VALUE
            }

            sokol.engine.call(setOf(entity), HostByEntityEvent)

            val tag = sokol.persistence.newTag()
            sokol.persistence.writeEntity(entity, tag)
            sokol.persistence.writeTagTo(tag, sokol.persistence.entityKey, mob.persistentDataContainer)
        }
    }
}
