package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.sokol.core.rule.*
import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val TYPE = "type"

class RuleSerializer(
    var types: Map<String, Class<Rule>> = emptyMap()
) : TypeSerializer<Rule> {
    override fun serialize(type: Type, obj: Rule?, node: ConfigurationNode) {}

    private fun noRuleType(type: Type, node: ConfigurationNode, key: String): Nothing =
        throw SerializationException(node, type, "No rule type for key '$key' - available: [${types.keys.joinToString()}]")

    override fun deserialize(type: Type, node: ConfigurationNode): Rule {
        if (node.isMap) {
            val key = node.node(TYPE).force<String>()
            val clazz = types[key] ?: noRuleType(type, node, key)
            return node.force(TypeToken.get(clazz))
        } else if (node.isList) {
            val args = node.childrenList().drop(1)

            fun <R> args(op: String, vararg argNames: String, action: List<ConfigurationNode>.() -> R): R {
                return if (argNames.size == args.size) action(args)
                else throw SerializationException(node, type, "Operator '$op' must be expressed as list of [${argNames.joinToString()}], found ${args.size}")
            }

            return when (val key = node.node(0).force<String>()) {
                "not" -> args("not", "rule") { NotRule(get(0).force()) }
                "all" -> AllRule(args.map { it.force() })
                "any" -> AnyRule(args.map { it.force() })

                "has" -> args("has", "path") { HasRule(get(0).force()) }
                "as" -> args("as", "path", "rule") { AsRule(get(0).force(), get(1).force()) }
                "as_root" -> args("as_root", "path", "rule") { AsRootRule(get(0).force(), get(1).force()) }
                "is_root" -> IsRootRule

                "has_tags" -> HasTagsRule(args.map { it.force<String>() }.toSet())
                "has_features" -> HasFeaturesRule(args.map { it.force<String>() }.toSet())
                "is_complete" -> IsCompleteRule
                else -> throw SerializationException(node, type, "No primitive rule of type '$key'")
            }
        } else {
            return when (val raw = node.raw()) {
                is Boolean -> Rule.of(raw)
                is String -> types[raw]?.let {
                    node.force(TypeToken.get(it))
                } ?: noRuleType(type, node, raw)
                else -> throw SerializationException(node, type, "Rule must be represented as map, list, boolean, or string")
            }
        }
    }
}
