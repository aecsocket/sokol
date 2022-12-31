package com.gitlab.aecsocket.sokol.paper.stat

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
import org.spongepowered.configurate.serialize.SerializationException
import java.text.DecimalFormat

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
data class DecimalStatFormatter(
    @Required override val stat: Stat<Double>,
    @Required override val nameKey: String,
    @Required val key: String,
    val mapper: RangeMapDouble = RangeMapDouble.Identity,
) : StatFormatter<Double> {
    override fun format(i18n: I18N<Component>, value: StatValue<Double>): TableRow<Component> {
        val number = mapper.map(value.compute())
        val text = i18n.safe(key) {
            icu("value", number)
        }
        return listOf(text)
    }
}
