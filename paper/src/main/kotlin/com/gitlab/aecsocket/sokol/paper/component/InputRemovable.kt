package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.bukkit.entity.Player

object InputRemovable : SimplePersistentComponent {
    override val componentType get() = InputRemovable::class
    override val key = SokolAPI.key("input_removable")
    val Type = ComponentType.singletonComponent(key, this)

    fun init(ctx: Sokol.InitContext) {
        ctx.persistentComponent(Type)
        ctx.system { InputRemovableSystem(it).init(ctx) }
    }
}

class InputRemovableSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Remove = InputRemovable.key.with("remove")
    }

    private val mInputRemovable = ids.mapper<InputRemovable>()
    private val mRemovable = ids.mapper<Removable>()
    private val mIsChild = ids.mapper<IsChild>()

    internal fun init(ctx: Sokol.InitContext): InputRemovableSystem {
        ctx.components.callbacks.apply {
            callback(Remove, ::remove)
        }
        return this
    }

    private fun remove(entity: SokolEntity, player: Player): Boolean {
        if (!mInputRemovable.has(entity)) return false

        val root = mIsChild.root(entity)
        val removable = mRemovable.getOr(root) ?: return false
        if (removable.removed) return true

        removable.remove()
        return true
    }
}
