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
import net.kyori.adventure.text.JoinConfiguration
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException

sealed interface NumberStatValue {
    val value: Number

    interface Set<V : Number> : NumberStatValue, StatNode.First<V>
    interface Add<V : Number> : NumberStatValue, StatNode<V>
    interface Multiply<V : Number> : NumberStatValue, StatNode<V>
}

data class IntegerStat(override val key: Key) : Stat<Int> {
    data class Set(override val value: Int) : NumberStatValue.Set<Int> {
        override fun with(last: Int) = value
        override fun first() = value
    }
    data class Add(override val value: Int) : NumberStatValue.Add<Int> {
        override fun with(last: Int) = last + value
    }

    private val operations = StatOperationSerializer(Int::class, mapOf(
        "=" to { Set(it) },
        "+" to { Add(it) }
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
    data class Multiply(override val value: Double) : NumberStatValue.Multiply<Double> {
        override fun with(last: Double) = last * value
    }

    private val operations = StatOperationSerializer(Double::class, mapOf(
        "=" to { Set(it) },
        "+" to { Add(it) },
        "*" to { Multiply(it) }
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

private const val ABSOLUTE = "absolute"
private const val RELATIVE = "relative"
private const val PERCENT = "percent"
private const val ADD = "add"
private const val SUBTRACT = "subtract"
private const val SEPARATOR = "separator"

enum class NumberSign(val key: String) {
    POSITIVE    ("positive"),
    NEGATIVE    ("negative"),
    NEUTRAL     ("neutral");

    fun opposite() = when (this) {
        POSITIVE -> NEGATIVE
        NEGATIVE -> POSITIVE
        NEUTRAL -> NEUTRAL
    }
}

@ConfigSerializable
data class NumberStatFormatter(
    @Required val key: String,
    val absolute: DisplaySettings = DisplaySettings(),
    val relative: DisplaySettings = DisplaySettings(),
    val percent: DisplaySettings = DisplaySettings()
) : StatFormatter<Number> {
    @ConfigSerializable
    data class DisplaySettings(
        val positive: NumberSign = NumberSign.NEUTRAL,
        val mapper: RangeMapDouble = RangeMapDouble.Identity
    )

    private fun key(path: String) = "$key.$path"

    override fun format(i18n: I18N<Component>, value: StatValue<Number>): Iterable<TableCell<Component>> {
        val separator = i18n.makeOne(key(SEPARATOR))
        val text = value.mapNotNull { node ->
            val rawNumber = (node as? NumberStatValue ?: return@mapNotNull null).value.toDouble()
            val settings = when (node) {
                is NumberStatValue.Set -> absolute
                is NumberStatValue.Add -> relative
                is NumberStatValue.Multiply -> percent
            }
            val number = settings.mapper.map(
                if (node is NumberStatValue.Multiply) rawNumber - 1 else rawNumber
            )

            val cell: Component
            val sign: NumberSign
            if (node is NumberStatValue.Set) {
                cell = i18n.safeOne(key(ABSOLUTE)) {
                    icu("value", number)
                }
                sign = NumberSign.NEUTRAL
            } else {
                fun numTextOf(number: Double) = i18n.safeOne(
                    key(if (node is NumberStatValue.Multiply) PERCENT else RELATIVE)
                ) {
                    icu("value", number)
                }

                when {
                    number > 0.0 -> {
                        cell = i18n.safeOne(key(ADD)) {
                            subst("value", numTextOf(number))
                        }
                        sign = settings.positive
                    }
                    number < 0.0 -> {
                        cell = i18n.safeOne(key(SUBTRACT)) {
                            subst("value", numTextOf(-number))
                        }
                        sign = settings.positive.opposite()
                    }
                    else -> {
                        cell = i18n.safeOne(key(ADD)) {
                            subst("value", numTextOf(number))
                        }
                        sign = NumberSign.NEUTRAL
                    }
                }
            }

            i18n.safeOne(key(sign.key)) {
                subst("cell", cell)
            }
        }.join(JoinConfiguration.separator(separator))
        return listOf(listOf(text))
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
