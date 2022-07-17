package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.alexandria.core.effect.SoundEffect
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.keyed.by
import com.gitlab.aecsocket.alexandria.core.physics.SimpleBody
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.Stat
import com.gitlab.aecsocket.sokol.core.util.RenderMesh
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

class NodeRenderException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

@ConfigSerializable
data class RenderData(
    val surfaceOffset: Double = 0.0,
    val partTransform: Transform = Transform.Identity,
    val attachedTransform: Transform = Transform.Identity,
    val attach: Attach? = null,

    val soundPlace: List<SoundEffect> = emptyList(),
    val soundGrab: List<SoundEffect> = emptyList(),
    val soundAttach: List<SoundEffect> = emptyList(),
    val soundDetach: List<SoundEffect> = emptyList(),
    val soundDragStart: List<SoundEffect> = emptyList(),
    val soundDragStop: List<SoundEffect> = emptyList(),
) {
    @ConfigSerializable
    data class Attach(
        @Required val axis: Vector3,
        @Required val maxDistance: Double,
        @Required val detachDistance: Double,
    )
}

@ConfigSerializable
data class RenderSlot(
    @Required val transform: Transform,
    val bodies: List<SimpleBody> = emptyList(),
)

object RenderFeature : Keyed {
    override val id get() = "render"

    abstract class Type<P : Feature.Profile<*>> : Feature<P> {
        override val id get() = RenderFeature.id

        override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
        override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()
    }

    abstract class Profile<D : Feature.Data<*>>(
        val bodies: Collection<SimpleBody>,
        val meshes: Collection<RenderMesh>,
        val slots: Map<String, RenderSlot>,
        val data: RenderData,
    ) : Feature.Profile<D> {
        abstract override val type: Type<*>
    }

    abstract class Data<S : Feature.State<S, *, *>> : Feature.Data<S> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        override fun serialize(tag: CompoundBinaryTag.Mutable) {}

        override fun serialize(node: ConfigurationNode) {}
    }

    abstract class State<
        S : Feature.State<S, D, C>,
        D : Feature.Data<S>,
        C : FeatureContext<*, H, N>,
        H : NodeHost,
        N
    > : Feature.State<S, D, C> where N : DataNode, N : Node.Mutable<N> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        override fun serialize(tag: CompoundBinaryTag.Mutable) {}

        abstract fun render(node: N, host: H, transform: Transform)
    }
}
