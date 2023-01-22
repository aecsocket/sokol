package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.sokol.core.SokolEntity
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

fun interface EntityRule {
    fun eval(entity: SokolEntity): Boolean
}

class ConstantEntityRule private constructor(val value: Boolean) : EntityRule {
    override fun eval(entity: SokolEntity) = value

    companion object {
        val True = ConstantEntityRule(true)
        val False = ConstantEntityRule(false)

        fun valueOf(value: Boolean) = if (value) True else False
    }
}

fun interface EntityRuleFactory {
    fun create(type: Type, node: ConfigurationNode): EntityRule
}

class EntityRules {
    private val _ruleTypes = HashMap<Key, EntityRuleFactory>()
    val ruleTypes: Map<Key, EntityRuleFactory> get() = _ruleTypes

    private val _ruleMacros = HashMap<String, EntityRuleFactory>()
    val ruleMacros: Map<String, EntityRuleFactory> get() = _ruleMacros

    fun ruleType(key: Key) = _ruleTypes[key]

    fun ruleType(key: Key, factory: EntityRuleFactory) {
        if (_ruleTypes.contains(key))
            throw IllegalArgumentException("Duplicate entity rule type $key")
        _ruleTypes[key] = factory
    }

    fun ruleMacro(key: String) = _ruleMacros[key]

    fun ruleMacro(key: String, factory: EntityRuleFactory) {
        if (_ruleMacros.contains(key))
            throw IllegalArgumentException("Duplicate entity rule macro $key")
        _ruleMacros[key] = factory
    }
}

inline fun <reified R : EntityRule> EntityRules.deserializingRuleType(key: Key) =
    ruleType(key) { _, node -> node.force<R>() }

inline fun <reified R : EntityRule> EntityRules.deserializingRuleMacro(key: String) =
    ruleMacro(key) { _, node -> node.force<R>() }

private const val TYPE = "type"

class EntityRuleSerializer(private val entityRules: EntityRules) : TypeSerializer<EntityRule> {
    override fun serialize(type: Type, obj: EntityRule?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): EntityRule {
        return when {
            node.raw() == true -> ConstantEntityRule.True
            node.raw() == false -> ConstantEntityRule.False
            node.isMap -> {
                val typeKey = node.node(TYPE).force<Key>()
                val ruleFactory = entityRules.ruleType(typeKey)
                    ?: throw SerializationException(node, type, "Invalid entity rule type '$typeKey'")
                ruleFactory.create(type, node)
            }
            node.isList -> {
                val list = node.childrenList()
                if (list.isEmpty())
                    throw SerializationException(node, type, "Entity rule as macro must define 1st element as macro key")
                val macroKey = list[0].force<String>()
                val ruleFactory = entityRules.ruleMacro(macroKey)
                    ?: throw SerializationException(node, type, "Invalid entity rule macro '$macroKey'")

                val copy = node.copy()
                copy.removeChild(0)
                ruleFactory.create(type, copy)
            }
            else -> throw SerializationException(node, type, "Invalid entity rule format")
        }
    }
}

@ConfigSerializable
data class AnyEntityRule(
    @Required @Setting(nodeFromParent = true) val terms: List<EntityRule>
) : EntityRule {
    companion object {
        const val Key = "any"
    }

    override fun eval(entity: SokolEntity): Boolean {
        return terms.any { it.eval(entity) }
    }
}

@ConfigSerializable
data class AllEntityRule(
    @Required @Setting(nodeFromParent = true) val terms: List<EntityRule>
) : EntityRule {
    companion object {
        const val Key = "all"
    }

    override fun eval(entity: SokolEntity): Boolean {
        return terms.all { it.eval(entity) }
    }
}
