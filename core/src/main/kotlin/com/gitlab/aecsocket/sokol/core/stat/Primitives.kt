package com.gitlab.aecsocket.sokol.core.stat

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.forceList
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

inline fun <reified T : Number> opDeserialize(
    node: ConfigurationNode,
    set: (T) -> NumberStat.Set<T>,
    add: (T) -> NumberStat.Add<T>,
    subtract: (T) -> NumberStat.Subtract<T>,
    multiply: (Double) -> NumberStat.Multiply<T>,
    divide: (Double) -> NumberStat.Divide<T>
): Stat.Value<T> {
    val type = T::class.java
    val list = node.forceList(type, "operator", "operand")
    val operand = list[1]
    return when (val operator = list[0].force<String>()) {
        "=" -> set(operand.force())
        "+" -> add(operand.force())
        "-" -> subtract(operand.force())
        "*" -> multiply(operand.force())
        "/" -> divide(operand.force())
        else -> throw SerializationException(node, type, "Invalid operator '$operator'")
    }
}

abstract class NumberStat<T : Number>(namespace: String, key: String) : AbstractStat<T>(namespace, key) {
    data class Set<T : Number>(val value: T) : Stat.Value.Discarding<T> {
        override fun first() = value
    }
    interface Add<T : Number> : Stat.Value.First<T>
    interface Subtract<T : Number> : Stat.Value.First<T>
    interface Multiply<T : Number> : Stat.Value.First<T>
    interface Divide<T : Number> : Stat.Value.First<T>
}

class DecimalStat(namespace: String, key: String) : NumberStat<Double>(namespace, key) {
    override fun deserialize(node: ConfigurationNode) = opDeserialize(node,
        { Set(it) }, { Add(it) }, { Subtract(it) }, { Multiply(it) }, { Divide(it) }
    )

    data class Add(val value: Double) : NumberStat.Add<Double> {
        override fun next(last: Double) = last + value
        override fun first() = value
    }
    data class Subtract(val value: Double) : NumberStat.Subtract<Double> {
        override fun next(last: Double) = last - value
        override fun first() = -value
    }
    data class Multiply(val value: Double) : NumberStat.Multiply<Double> {
        override fun next(last: Double) = last * value
        override fun first() = value
    }
    data class Divide(val value: Double) : NumberStat.Divide<Double> {
        override fun next(last: Double) = last / value
        override fun first() = 1.0 / value
    }
}

class IntegerStat(namespace: String, key: String) : NumberStat<Long>(namespace, key) {
    override fun deserialize(node: ConfigurationNode) = opDeserialize(node,
        { Set(it) }, { Add(it) }, { Subtract(it) }, { Multiply(it) }, { Divide(it) }
    )

    data class Add(val value: Long) : NumberStat.Add<Long> {
        override fun next(last: Long) = last + value
        override fun first() = value
    }
    data class Subtract(val value: Long) : NumberStat.Subtract<Long> {
        override fun next(last: Long) = last - value
        override fun first() = -value
    }
    data class Multiply(val value: Double) : NumberStat.Multiply<Long> {
        override fun next(last: Long) = (last * value).toLong()
        override fun first() = value.toLong()
    }
    data class Divide(val value: Double) : NumberStat.Divide<Long> {
        override fun next(last: Long) = (last / value).toLong()
        override fun first() = (1.0 / value).toLong()
    }
}
