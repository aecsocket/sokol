package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.extension.forceList
import com.github.aecsocket.alexandria.core.extension.forceMap
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.stat.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val PRIORITY = "priority"
private const val REVERSED = "reversed"
private const val RULE = "rule"

object ApplicableStatsSerializer : TypeSerializer<ApplicableStats> {
    override fun serialize(type: Type, obj: ApplicableStats?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): ApplicableStats {
        val priority = node.node(PRIORITY).get { 0 }
        val reversed = node.node(REVERSED).get { false }
        val rule = node.node(RULE).get<Rule> { Rule.True }
        val stats = node.copy().apply {
            removeChild(PRIORITY)
            removeChild(REVERSED)
            removeChild(RULE)
        }
        return ApplicableStats(stats.force(), priority, reversed, rule)
    }
}
