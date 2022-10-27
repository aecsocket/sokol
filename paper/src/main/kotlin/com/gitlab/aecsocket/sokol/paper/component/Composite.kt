package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
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
        val children: Map<String, EntityProfile>
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound().run { Composite(sokol,
            associate { (key, tag) -> tag.asCompound().run {
                val blueprint = children[key]?.let { profile ->
                    sokol.persistence.readBlueprintByProfile(profile, this)
                } ?: sokol.persistence.readBlueprint(this)
                key to sokol.engine.buildEntity(blueprint)
            } }.toMutableMap()
        ) }

        override fun read(node: ConfigurationNode) = Composite(sokol,
            node.childrenMap().map { (key, child) ->
                val blueprint = children[key.toString()]?.let { profile ->
                    deserializeBlueprintByProfile(sokol, profile, child)
                } ?: child.force()
                key.toString() to sokol.engine.buildEntity(blueprint)
            }.toMap().toMutableMap()
        )
    }

    class Type(private val sokol: Sokol) : ComponentType {
        override val key get() = Key

        override fun createProfile(node: ConfigurationNode) = Profile(sokol,
            node.force<HashMap<String, EntityProfile>>())
    }
}

fun ComponentMapper<Composite>.forEachChild(entity: SokolEntity, action: (Map.Entry<String, SokolEntity>) -> Unit) {
    getOr(entity)?.children?.forEach(action)
}

fun ComponentMapper<Composite>.forward(entity: SokolEntity, event: SokolEvent) {
    forEachChild(entity) { (_, child) ->
        child.call(event)
    }
}
