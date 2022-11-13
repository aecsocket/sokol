package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.*
import com.gitlab.aecsocket.sokol.paper.util.spawnMarkerMob
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

class EntityHoster internal constructor(
    private val sokol: Sokol
) {
    private lateinit var mAsMob: ComponentMapper<AsMob>
    private lateinit var mAsItem: ComponentMapper<AsItem>
    private lateinit var mRotation: ComponentMapper<Rotation>
    private lateinit var mIsMob: ComponentMapper<IsMob>
    private lateinit var mIsItem: ComponentMapper<IsItem>

    internal fun enable() {
        mAsMob = sokol.engine.mapper()
        mAsItem = sokol.engine.mapper()
        mRotation = sokol.engine.mapper()
        mIsMob = sokol.engine.mapper()
        mIsItem = sokol.engine.mapper()
    }

    fun hostMob(blueprint: EntityBlueprint, location: Location): Entity {
        val space = sokol.engine.emptySpace()
        val entity = blueprint.createIn(space)
        return spawnMarkerMob(location) { mob ->
            mIsMob.set(entity, IsMob(mob))
            space.construct()
            space.call(MobEvent.Spawn)
            sokol.mobsAdded.add(mob.entityId)
            sokol.persistence.writeEntityTagTo(entity, mob.persistentDataContainer)
        }
    }

    fun hostMob(blueprint: EntityBlueprint, world: World, transform: Transform): Entity {
        val location = transform.translation.location(world)
        return hostMob(blueprint.with(mRotation) { Rotation(transform.rotation) }, location)
    }

    fun hostItem(blueprint: EntityBlueprint): ItemStack {
        val space = sokol.engine.emptySpace()
        val entity = blueprint.createIn(space)

        val item = mAsItem.get(entity).profile.item.create()
        return item.withMeta { meta ->
            mIsItem.set(entity, IsItem({ item }, { meta }))
            space.construct()
            space.call(ItemEvent.CreateForm)
            space.call(ItemEvent.Create)
            sokol.persistence.writeEntityTagTo(entity, meta.persistentDataContainer)
        }
    }

    fun createItemForm(blueprint: EntityBlueprint): ItemStack {
        val space = sokol.engine.emptySpace()
        val entity = blueprint.createIn(space)

        val item = mAsItem.get(entity).profile.item.create()
        return item.withMeta { meta ->
            mIsItem.set(entity, IsItem({ item }, { meta }))
            space.construct()
            space.call(ItemEvent.CreateForm)
        }
    }
}
