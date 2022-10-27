package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode

object Forwarding : PersistentComponent {
    override val componentType get() = Forwarding::class
    override val key = SokolAPI.key("forwarding")
    val Type = ComponentType.singletonComponent(key, Forwarding)

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}
}

@All(Forwarding::class, Composite::class)
class ForwardingSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: SokolEvent, entity: SokolEntity) {
        mComposite.forward(entity, event)
    }
}
