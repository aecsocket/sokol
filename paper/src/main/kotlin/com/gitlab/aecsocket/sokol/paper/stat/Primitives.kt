package com.gitlab.aecsocket.sokol.paper.stat

import com.gitlab.aecsocket.alexandria.core.BarRenderer
import com.gitlab.aecsocket.alexandria.core.RangeMapDouble
import com.gitlab.aecsocket.alexandria.core.TableCell
import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.paper.component.*
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException

interface NumberStatValue {
    val value: Number

    interface Set<V : Number> : NumberStatValue, StatNode.First<V>
    interface Add<V : Number> : NumberStatValue, StatNode<V>
    interface Subtract<V : Number> : NumberStatValue, StatNode<V>
    interface Multiply<V : Number> : NumberStatValue, StatNode<V>
    interface Divide<V : Number> : NumberStatValue, StatNode<V>
}

data class IntegerStat(override val key: Key) : Stat<Int> {
    data class Set(override val value: Int) : NumberStatValue.Set<Int> {
        override fun with(last: Int) = value
        override fun first() = value
    }
    data class Add(override val value: Int) : NumberStatValue.Add<Int> {
        override fun with(last: Int) = last + value
    }
    data class Subtract(override val value: Int) : NumberStatValue.Subtract<Int> {
        override fun with(last: Int) = last - value
    }

    private val operations = StatOperationSerializer(Int::class, mapOf(
        "=" to { Set(it) },
        "+" to { Add(it) },
        "-" to { Subtract(it) }
    ))

    override fun createNode(node: ConfigurationNode) = operations.createValue(node)
}

data class IntegerCounterStat(override val key: Key) : Stat<Int> {
    data class Value(override val value: Int) : NumberStatValue.Set<Int> {
        override fun with(last: Int) = last + value
        override fun first() = value
    }

    override fun createNode(node: ConfigurationNode): StatNode<Int> {
        val value = node.force<Int>()
        return if (value >= 0) Value(value)
        else throw SerializationException(node, Int::class.java, "Value for counter stat must be zero or positive")
    }
}

data class DecimalStat(override val key: Key) : Stat<Double> {
    data class Set(override val value: Double) : NumberStatValue.Set<Double> {
        override fun with(last: Double) = value
        override fun first() = value
    }
    data class Add(override val value: Double) : NumberStatValue.Add<Double> {
        override fun with(last: Double) = last + value
    }
    data class Subtract(override val value: Double) : NumberStatValue.Subtract<Double> {
        override fun with(last: Double) = last - value
    }
    data class Multiply(override val value: Double) : NumberStatValue.Multiply<Double> {
        override fun with(last: Double) = last * value
    }
    data class Divide(override val value: Double) : NumberStatValue.Divide<Double> {
        override fun with(last: Double) = last / value
    }

    private val operations = StatOperationSerializer(Double::class, mapOf(
        "=" to { Set(it) },
        "+" to { Add(it) },
        "-" to { Subtract(it) },
        "*" to { Multiply(it) },
        "/" to { Divide(it) }
    ))

    override fun createNode(node: ConfigurationNode) = operations.createValue(node)
}

data class DecimalCounterStat(override val key: Key) : Stat<Double> {
    data class Value(override val value: Double) : NumberStatValue.Set<Double> {
        override fun with(last: Double) = last + value
        override fun first() = value
    }

    override fun createNode(node: ConfigurationNode): StatNode<Double> {
        val value = node.force<Double>()
        return if (value >= 0) Value(value)
        else throw SerializationException(node, Double::class.java, "Value for counter stat must be zero or positive")
    }
}

@ConfigSerializable
data class NameStatFormatter(
    @Required val key: String
) : StatFormatter<Any> {
    override fun format(i18n: I18N<Component>, value: StatValue<Any>): Iterable<TableCell<Component>> {
        val text = i18n.safe(key)
        return listOf(text)
    }
}

private const val ABSOLUTE = "absolute"
private const val PERCENT = "percent"
private const val LINE = "line"
private const val SET = "set"
private const val ADD = "add"
private const val SUBTRACT = "subtract"
private const val MULTIPLY = "multiply"
private const val DIVIDE = "divide"

@ConfigSerializable
data class NumberStatFormatter(
    @Required val key: String,
    val asPercent: Boolean = false,
    val mapper: RangeMapDouble = RangeMapDouble.Identity
) : StatFormatter<Number> {
    override fun format(i18n: I18N<Component>, value: StatValue<Number>): Iterable<TableCell<Component>> {
        val nodeText = value.mapNotNull { node ->
            val number = (node as? NumberStatValue ?: return@mapNotNull null).value

            val text = i18n.safeOne(
                if (asPercent && node is NumberStatValue.Multiply) "$key.$PERCENT"
                else "$key.$ABSOLUTE"
            ) {
                icu("value", number)
            }

            i18n.safeOne("$key.${when (node) {
                is NumberStatValue.Add -> ADD
                is NumberStatValue.Subtract -> SUBTRACT
                is NumberStatValue.Multiply -> MULTIPLY
                is NumberStatValue.Divide -> DIVIDE
                else -> SET
            }}") {
                subst("value", text)
            }
        }.join()
        val text = i18n.safe("$key.$LINE") {
            subst("value", nodeText)
        }
        return listOf(text)
    }
}

@ConfigSerializable
data class StatBarData(
    @Required @Setting(nodeFromParent = true) val renderer: BarRenderer,
    @Required val max: Double
)

@ConfigSerializable
data class NumberStatBarFormatter(
    @Required val key: String,
    @Required val bar: StatBarData,
    val mapper: RangeMapDouble = RangeMapDouble.Identity
) : StatFormatter<Number> {
    override fun format(i18n: I18N<Component>, value: StatValue<Number>): Iterable<TableCell<Component>> {
        // we can't work without an absolute value
        val number = value.computeOr()?.let { mapper.map(it.toDouble()) } ?: return emptyList()
        val percent = number / bar.max
        val (first, background) = bar.renderer.renderOne(percent.toFloat())
        val text = i18n.safe(key) {
            subst("first", first)
            subst("background", background)
        }
        return listOf(text)
    }
}
