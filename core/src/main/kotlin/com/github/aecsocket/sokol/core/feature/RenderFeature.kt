package com.github.aecsocket.sokol.core.feature

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.alexandria.core.physics.SimpleBody
import com.github.aecsocket.alexandria.core.physics.Transform
import com.github.aecsocket.alexandria.core.physics.Vector3
import com.github.aecsocket.sokol.core.*
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.stat.Stat
import com.github.aecsocket.sokol.core.util.RenderMesh
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

class NodeRenderException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

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
        val slots: Map<String, Transform>,
        val attachedTransform: Transform,
        val snapTransform: Transform,
        val attachAxis: Vector3,
        val attachDistance: Double,
    ) : Feature.Profile<D> {
        abstract override val type: Type<*>
    }

    abstract class Data<S : Feature.State<S, *, *>> : Feature.Data<S> {
        abstract override val type: Feature<*>

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

        override fun onEvent(event: NodeEvent, ctx: C) {
            // TODO on spawn into world (eg dropped by a player)
            // NOTE: this must spawn as a render ONLY IF it's appropriate
            // e.g. a player drops a single item, not 2+
            // and NOT if a chest breaks full of these items, or similar
        }

        override fun serialize(tag: CompoundBinaryTag.Mutable) {}

        abstract fun render(node: N, host: H, transform: Transform)
    }
}
