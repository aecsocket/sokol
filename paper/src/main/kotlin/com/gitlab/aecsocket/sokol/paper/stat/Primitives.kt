package com.gitlab.aecsocket.sokol.paper.stat

import com.gitlab.aecsocket.alexandria.core.BarRenderer
import com.gitlab.aecsocket.alexandria.core.RangeMapDouble
import com.gitlab.aecsocket.alexandria.core.TableRow
import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.paper.component.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException

data class IntegerStat(override val key: Key) : Stat<Int> {
    data class Set(val value: Int) : StatNode.First<Int> {
        override fun with(last: Int) = value
        override fun first() = value
    }
    data class Add(val value: Int) : StatNode<Int> {
        override fun with(last: Int) = last + value
    }
    data class Subtract(val value: Int) : StatNode<Int> {
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
    data class Add(val value: Int) : StatNode.First<Int> {
        override fun with(last: Int) = last + value
        override fun first() = value
    }

    override fun createNode(node: ConfigurationNode): StatNode<Int> {
        val value = node.force<Int>()
        return if (value >= 0) Add(value)
        else throw SerializationException(node, Int::class.java, "Value for counter stat must be zero or positive")
    }
}

data class DecimalStat(override val key: Key) : Stat<Double> {
    data class Set(val value: Double) : StatNode.First<Double> {
        override fun with(last: Double) = value
        override fun first() = value
    }
    data class Add(val value: Double) : StatNode<Double> {
        override fun with(last: Double) = last + value
    }
    data class Subtract(val value: Double) : StatNode<Double> {
        override fun with(last: Double) = last - value
    }
    data class Multiply(val value: Double) : StatNode<Double> {
        override fun with(last: Double) = last * value
    }
    data class Divide(val value: Double) : StatNode<Double> {
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
    data class Add(val value: Double) : StatNode.First<Double> {
        override fun with(last: Double) = last + value
        override fun first() = value
    }

    override fun createNode(node: ConfigurationNode): StatNode<Double> {
        val value = node.force<Double>()
        return if (value >= 0) Add(value)
        else throw SerializationException(node, Double::class.java, "Value for counter stat must be zero or positive")
    }
}

@ConfigSerializable
data class NameStatFormatter(
    @Required val key: String
) : StatFormatter<Any> {
    override fun format(i18n: I18N<Component>, value: StatValue<Any>): TableRow<Component> {
        val text = i18n.safe(key)
        return listOf(text)
    }
}

@ConfigSerializable
data class NumberStatFormatter(
    @Required val key: String,
    val mapper: RangeMapDouble = RangeMapDouble.Identity
) : StatFormatter<Number> {
    override fun format(i18n: I18N<Component>, value: StatValue<Number>): TableRow<Component> {
        val number = mapper.map(value.compute().toDouble())
        val text = i18n.safe(key) {
            icu("value", number)
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
    override fun format(i18n: I18N<Component>, value: StatValue<Number>): TableRow<Component> {
        val number = mapper.map(value.compute().toDouble())
        val percent = number / bar.max
        val (first, background) = bar.renderer.renderOne(percent.toFloat())
        val text = i18n.safe(key) {
            subst("first", first)
            subst("background", background)
        }
        return listOf(text)
    }
}
