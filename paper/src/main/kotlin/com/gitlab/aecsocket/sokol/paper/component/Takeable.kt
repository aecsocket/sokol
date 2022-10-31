package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.colliderHitPath
import org.spongepowered.configurate.ConfigurationNode
import java.util.*

object Takeable : PersistentComponent {
    override val componentType get() = Takeable::class
    override val key = SokolAPI.key("takeable")
    val Type = ComponentType.singletonComponent(key, Takeable)

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}
}

@All(Takeable::class)
class TakeableSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    companion object {
        val TakeAsItem = SokolAPI.key("takeable/take_as_item")
    }

    private val mHovered = mappers.componentMapper<Hovered>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mComposite = mappers.componentMapper<Composite>()
    private val mRemovable = mappers.componentMapper<Removable>()
    private val mAsItem = mappers.componentMapper<HostableByItem>()

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        event.addAction(TakeAsItem) { (player, _, cancel) ->
            cancel()

            val hitPath = mHovered.getOr(entity)?.let {
                colliderHitPath(mCollider.getOr(entity), it.rayTestResult)
            } ?: emptyCompositePath()

            val hitEntity: SokolEntity = if (hitPath.isEmpty()) {
                val removable = mRemovable.getOr(entity) ?: return@addAction true
                if (!mAsItem.has(entity)) return@addAction true
                removable.remove()
                entity
            } else {
                val parentPath = hitPath.toMutableList()
                val childKey = parentPath.removeLast()
                val parent = mComposite.child(entity, parentPath) ?: return@addAction true
                val parentComposite = mComposite.get(parent)
                val child = parentComposite.child(childKey) ?: return@addAction true
                if (!mAsItem.has(child)) return@addAction true

                parentComposite.detach(parent, childKey)!!.also {
                    entity.call(Composite.TreeMutate)
                }
            }

            hitEntity.call(SokolEvent.Remove)
            val item = sokol.entityHoster.hostItem(hitEntity.toBlueprint())
            player.give(item)
            true
        }
    }
}
