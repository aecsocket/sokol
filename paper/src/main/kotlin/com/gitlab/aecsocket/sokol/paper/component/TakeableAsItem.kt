package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.bukkit.entity.Player

object TakeableAsItem : SimplePersistentComponent {
    override val componentType get() = TakeableAsItem::class
    override val key = SokolAPI.key("takeable_as_item")
    val Type = ComponentType.singletonComponent(key, this)

    fun init(ctx: Sokol.InitContext) {
        ctx.persistentComponent(Type)
        ctx.system { TakeableAsItemSystem(ctx.sokol, it).init(ctx) }
    }
}

class TakeableAsItemSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Take = TakeableAsItem.key.with("take")
    }

    private val mTakeableAsItem = ids.mapper<TakeableAsItem>()
    private val mRemovable = ids.mapper<Removable>()
    private val mIsChild = ids.mapper<IsChild>()

    internal fun init(ctx: Sokol.InitContext): TakeableAsItemSystem {
        ctx.components.callbacks.apply {
            callback(Take, ::take)
        }
        return this
    }

    private fun take(entity: SokolEntity, player: Player): Boolean {
        if (!mTakeableAsItem.has(entity)) return false

        val root = mIsChild.root(entity)
        val removable = mRemovable.getOr(root) ?: return false
        if (removable.removed) return true

        removable.remove()
        val item = sokol.hoster.hostItem(sokol.persistence.blueprintOf(root).create())
        player.inventory.addItem(item)
        return true
    }
}
