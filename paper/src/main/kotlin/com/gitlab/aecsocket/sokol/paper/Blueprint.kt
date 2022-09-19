package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftAreaEffectCloud
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack

/*
class ItemBlueprint(private val sokol: Sokol, val backing: SokolBlueprint) {
    init {
        if (!backing.containsType(HostableByEntity))
    }

    fun createItem(space: SokolEngine.Space): ItemStack {
        val space = sokol.engine.createSpace(1)
        val entity = backing.create(space)



        space.addComponent(entity, hostedByItem())


        space.addComponent(entity, hostedByItem())

        val entity = space.createEntity(backing.archetype)


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
}*/

open class EntityBlueprint(private val sokol: Sokol, val backing: SokolBlueprint) {
    init {
        if (!backing.containsType(HostableByEntity))
            throw IllegalArgumentException("Backing blueprint must have component of type ${HostableByEntity::class}")
    }

    fun spawnEntity(location: Location): Entity {
        return location.world.spawnEntity(
            location, EntityType.AREA_EFFECT_CLOUD, CreatureSpawnEvent.SpawnReason.CUSTOM
        ) { mob ->
            (mob as CraftAreaEffectCloud).handle.apply {
                tickCount = Int.MIN_VALUE
                duration = -1
                waitTime = Int.MIN_VALUE
            }

            val space = sokol.engine.createSpace(1)
            val entity = backing.create(space)
            space.addComponent(entity, hostedByEntity(mob))
            space.call(HostByEntityEvent)

            val tag = sokol.persistence.newTag()
            sokol.persistence.writeEntity(space, entity, tag)
            sokol.persistence.writeTagTo(tag, sokol.persistence.entityKey, mob.persistentDataContainer)
        }
    }
}

class KeyedEntityBlueprint(
    sokol: Sokol,
    override val id: String,
    backing: SokolBlueprint
) : EntityBlueprint(sokol, backing), Keyed
