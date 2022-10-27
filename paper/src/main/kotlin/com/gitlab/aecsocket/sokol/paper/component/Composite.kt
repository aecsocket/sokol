package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.asBlueprint
import com.gitlab.aecsocket.sokol.paper.util.makeEntity
import org.spongepowered.configurate.ConfigurationNode
import kotlin.collections.HashMap

data class Composite(
    private val sokol: Sokol,
    val children: MutableMap<String, SokolEntity> = HashMap(),
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("composite")
    }

    override val componentType get() = Composite::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound().apply {
        children.forEach { (key, entity) ->
            set(key) { makeEntity(sokol, entity) }
        }
    }

    override fun write(node: ConfigurationNode) {
        children.forEach { (key, entity) ->
            node.node(key).set(entity)
        }
    }

    class Profile(
        private val sokol: Sokol,
        val children: Map<String, EntityBlueprint>
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound().run { Composite(sokol,
            associate { (key, tag) ->
                key to sokol.engine.buildEntity(tag.asBlueprint(sokol))
            }.toMutableMap()
        ) }

        override fun read(node: ConfigurationNode) = Composite(sokol,
            node.childrenMap().map { (key, child) ->
                key.toString() to sokol.engine.buildEntity(child.force())
            }.toMap().toMutableMap()
        )
    }

    class Type(private val sokol: Sokol) : ComponentType {
        override val key get() = Key

        override fun createProfile(node: ConfigurationNode) = Profile(sokol, emptyMap()) // todo
    }

    data class Forwarded(
        val event: SokolEvent,
        val childId: String,
        val parent: SokolEntity,
        val root: SokolEntity
    ) : SokolEvent
}

@All(Composite::class)
class CompositeSystem(engine: SokolEngine) : SokolSystem {
    private val mComposite = engine.componentMapper<Composite>()

    @Subscribe
    fun on(event: SokolEvent, entity: SokolEntity) {
        val composite = mComposite.map(entity)

        composite.children.forEach { (id, child) ->
            child.call(Composite.Forwarded(
                event, id, entity,
                if (event is Composite.Forwarded) event.root else entity
            ))
        }
    }
}

fun ComponentMapper<Composite>.forEachChild(entity: SokolEntity, action: (Map.Entry<String, SokolEntity>) -> Unit) {
    mapOr(entity)?.children?.forEach(action)
}

fun ComponentMapper<Composite>.forward(entity: SokolEntity, event: SokolEvent) {
    forEachChild(entity) { (_, child) ->
        child.call(event)
    }
}
