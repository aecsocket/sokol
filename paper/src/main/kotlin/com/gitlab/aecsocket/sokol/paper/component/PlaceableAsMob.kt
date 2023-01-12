package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.transform
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ItemEvent
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.bukkit.GameMode

object PlaceableAsMob : SimplePersistentComponent {
    override val componentType get() = PlaceableAsMob::class
    override val key = SokolAPI.key("placeable_as_mob")
    val Type = ComponentType.singletonComponent(key, this)
}

@All(PlaceableAsMob::class, IsItem::class, AsMob::class, Holdable::class)
class PlaceableAsMobSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mIsItem = ids.mapper<IsItem>()

    @Subscribe
    fun on(event: ItemEvent.ClickAsCurrent, entity: SokolEntity) {
        val isItem = mIsItem.get(entity)
        if (!event.isRightClick || !event.isShiftClick) return

        val player = event.player
        event.cancel()

        if (player.gameMode != GameMode.CREATIVE) {
            isItem.item.subtract()
        }

        player.closeInventory()
        val mobEntity = sokol.persistence.blueprintOf(entity).create()
        val location = player.eyeLocation
        sokol.hoster.hostMob(mobEntity, location)
        sokol.holding.start(player.alexandria, mobEntity, MoveHoldOperation(), location.transform())
    }
}
