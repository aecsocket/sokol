package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.glossa.core.I18N
import com.github.aecsocket.glossa.core.Localizable
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.nbt.TagSerializable
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode

interface Feature<
    P : Feature.Profile<*>
> : Keyed, Localizable<Component> {
    fun createProfile(node: ConfigurationNode): P

    override fun localize(i18n: I18N<Component>) =
        i18n.safe("feature.$id")

    interface Profile<D : Data<*>> {
        val type: Feature<*>

        fun createData(): D

        fun createData(node: ConfigurationNode): D

        fun createData(tag: CompoundBinaryTag): D
    }

    interface Data<S : State<S, *, *>> : TagSerializable {
        val type: Feature<*>

        fun createState(): S

        fun serialize(node: ConfigurationNode)
    }

    interface State<
        S : State<S, D, C>,
        D : Data<S>,
        C : FeatureContext<*, *, *>
    > : TagSerializable {
        val type: Feature<*>

        fun asData(): D

        fun resolveDependencies(get: (String) -> S?)

        fun onEvent(event: NodeEvent, ctx: C)
    }
}

interface FeatureContext<
    S : TreeState,
    H : NodeHost,
    N
> where N : DataNode, N : Node.Mutable<N> {
    val state: S
    val host: H
    val node: DataNode

    fun writeNode(action: N.() -> Unit)
}
