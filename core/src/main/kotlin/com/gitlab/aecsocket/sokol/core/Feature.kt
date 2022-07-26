package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.glossa.core.Localizable
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.nbt.TagSerializable
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.ApplicableStats
import com.gitlab.aecsocket.sokol.core.stat.Stat
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode

data class FeatureKey(val id: String)

interface Feature<
    P : Feature.Profile<*>
> : Keyed, Localizable<Component> {
    val statTypes: Map<Key, Stat<*>>

    val ruleTypes: Map<Key, Class<Rule>>

    fun createProfile(node: ConfigurationNode): P

    override fun localize(i18n: I18N<Component>) =
        i18n.safe("feature.$id")

    interface Profile<D : Data<*>> : Keyed {
        val type: Feature<*>
        override val id get() = type.id

        fun createData(): D

        fun createData(node: ConfigurationNode): D

        fun createData(tag: CompoundBinaryTag): D
    }

    interface Data<S : State<S, *, *>> : TagSerializable, Keyed {
        val type: Feature<*>
        val profile: Profile<*>
        override val id get() = type.id

        fun createState(): S

        fun serialize(node: ConfigurationNode)
    }

    interface State<
        S : State<S, D, C>,
        D : Data<S>,
        C : FeatureContext<*, *, *>
    > : Keyed {
        val type: Feature<*>
        val profile: Profile<*>
        override val id get() = type.id

        fun asData(): D

        fun resolveDependencies(get: (String) -> S?) {}

        fun createStats(): List<ApplicableStats> = emptyList()

        fun onEvent(event: NodeEvent, ctx: C)
    }
}

fun Feature.State<*, *, *>.tlKey(key: String) = "feature.$id.$key"

interface FeatureContext<
    S : TreeState,
    H : NodeHost<N>,
    N
> where N : DataNode, N : Node.Mutable<N> {
    val state: S
    val host: H
    val node: N

    fun writeNode(action: N.() -> Unit)

    fun updateHost()
}
