package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting

data class Tagged(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("tagged")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { TaggedSystem(it, ctx) }
        }
    }

    override val componentType get() = Tagged::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required @Setting(nodeFromParent = true) val tags: Set<String>
    ) : SimpleComponentProfile<Tagged> {
        override val componentType get() = Tagged::class

        override fun createEmpty() = ComponentBlueprint { Tagged(this) }
    }
}

class TaggedSystem(ids: ComponentIdAccess, ctx: Sokol.InitContext) : SokolSystem {
    companion object {
        const val Tagged = "tagged"
    }

    @ConfigSerializable
    data class RuleData(
        @Required @Setting(nodeFromParent = true) val tags: Set<String>
    )

    private val mTagged = ids.mapper<Tagged>()

    init {
        ctx.components.rules.ruleMacro(Tagged) { _, node ->
            val (tags) = node.force<RuleData>()
            EntityRule { entity ->
                val testTags = mTagged.getOr(entity)?.profile?.tags ?: return@EntityRule false
                return@EntityRule testTags.intersect(tags).isNotEmpty()
            }
        }
    }
}
