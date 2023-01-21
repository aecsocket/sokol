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
        ctx.system { TakeableAsItemSystem(ctx.sokol, it) }
    }
}

@All(TakeableAsItem::class, InputCallbacks::class, Removable::class, AsItem::class)
@Before(InputCallbacksSystem::class)
@After(RemovableTarget::class)
class TakeableAsItemSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Take = TakeableAsItem.key.with("take")
    }

    data class Remove(
        val player: Player
    ) : SokolEvent

    private val mInputCallbacks = ids.mapper<InputCallbacks>()
    private val mRemovable = ids.mapper<Removable>()
    private val mIsChild = ids.mapper<IsChild>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val inputCallbacks = mInputCallbacks.get(entity)

        inputCallbacks.callback(Take) { player ->
            mIsChild.root(entity).call(Remove(player))
            true
        }
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val removable = mRemovable.get(entity)
        if (removable.removed) return
        removable.remove()

        val item = sokol.hoster.hostItem(sokol.persistence.blueprintOf(entity).create())
        event.player.inventory.addItem(item)
    }
}
