package com.gitlab.aecsocket.sokol.core

import java.util.UUID

private fun wrongType(expected: String): Nothing {
    throw IllegalStateException("Expected tag of type $expected")
}

interface NBTTagContext {
    fun makeBoolean(value: Boolean): BooleanNBTTag
    fun makeInt(value: Int): NumericNBTTag
    fun makeLong(value: Long): NumericNBTTag
    fun makeByte(value: Byte): NumericNBTTag
    fun makeShort(value: Short): NumericNBTTag
    fun makeFloat(value: Float): NumericNBTTag
    fun makeDouble(value: Double): NumericNBTTag
    fun makeString(value: String): StringNBTTag
    fun makeUUID(value: UUID): UUIDNBTTag
    fun makeCompound(): CompoundNBTTag.Mutable
    fun makeIntArray(values: IntArray): IntArrayNBTTag
    fun makeLongArray(values: LongArray): LongArrayNBTTag
    fun makeByteArray(values: ByteArray): ByteArrayNBTTag
    fun makeFloatArray(values: FloatArray): FloatArrayNBTTag
    fun makeDoubleArray(values: DoubleArray): DoubleArrayNBTTag
    fun makeList(): ListNBTTag.Mutable
}

interface NBTTag : NBTTagContext {
    fun asBoolean() = (this as? BooleanNBTTag)?.boolean ?: wrongType("boolean")
    fun asInt() = (this as? NumericNBTTag)?.int ?: wrongType("int")
    fun asLong() = (this as? NumericNBTTag)?.long ?: wrongType("long")
    fun asByte() = (this as? NumericNBTTag)?.byte ?: wrongType("byte")
    fun asShort() = (this as? NumericNBTTag)?.short ?: wrongType("short")
    fun asFloat() = (this as? NumericNBTTag)?.float ?: wrongType("short")
    fun asDouble() = (this as? NumericNBTTag)?.double ?: wrongType("double")
    fun asString() = (this as? StringNBTTag)?.string ?: wrongType("string")
    fun asUUID() = (this as? UUIDNBTTag)?.uuid ?: wrongType("uuid")
    fun asCompound() = this as? CompoundNBTTag ?: wrongType("compound")
    fun asIntArray() = (this as? IntArrayNBTTag)?.intArray ?: wrongType("int_array")
    fun asLongArray() = (this as? LongArrayNBTTag)?.longArray ?: wrongType("long_array")
    fun asByteArray() = (this as? ByteArrayNBTTag)?.byteArray ?: wrongType("byte_array")
    fun asFloatArray() = (this as? FloatArrayNBTTag)?.floatArray ?: wrongType("float_array")
    fun asDoubleArray() = (this as? DoubleArrayNBTTag)?.doubleArray ?: wrongType("double_array")
    fun asList() = this as? ListNBTTag ?: wrongType("list")
}

interface BooleanNBTTag : NBTTag {
    val boolean: Boolean
}

interface NumericNBTTag : NBTTag {
    val int: Int
    val long: Long
    val byte: Byte
    val short: Short
    val float: Float
    val double: Double
}

interface StringNBTTag : NBTTag {
    val string: String
}

interface UUIDNBTTag : NBTTag {
    val uuid: UUID
}

interface CompoundNBTTag : NBTTag, Iterable<Pair<String, NBTTag>> {
    val size: Int
    val keys: Set<String>
    val map: Map<String, NBTTag>

    fun contains(key: String): Boolean

    operator fun get(key: String): NBTTag?

    fun <R> get(key: String, mapper: NBTTag.() -> R): R {
        val tag = get(key) ?: throw IllegalStateException("Requires key '$key'")
        return try {
            mapper(tag)
        } catch (ex: IllegalStateException) {
            throw IllegalStateException("Invalid key '$key': ${ex.message}")
        }
    }

    fun <R> getOr(key: String, mapper: NBTTag.() -> R?): R? {
        return get(key)?.let { tag ->
            try {
                mapper(tag)
            } catch (ex: IllegalStateException) {
                null
            }
        }
    }

    fun getList(key: String): Iterable<NBTTag> = getOr(key) { asList() } ?: emptySet()

    interface Mutable : CompoundNBTTag {
        operator fun set(key: String, tag: NBTTag): Mutable

        fun set(key: String, tagCreator: NBTTag.() -> NBTTag): Mutable

        fun setOrClear(key: String, tagCreator: NBTTag.() -> NBTTag?): Mutable

        fun remove(key: String): Mutable

        fun clear(): Mutable
    }
}

interface CollectionNBTTag : NBTTag {
    val size: Int
}

interface IntArrayNBTTag : CollectionNBTTag {
    val intArray: IntArray

    fun getInt(index: Int): Int

    fun setInt(index: Int, value: Int): IntArrayNBTTag

    fun asIntIterable(): Iterable<Int>
}

interface LongArrayNBTTag : CollectionNBTTag {
    val longArray: LongArray

    fun getLong(index: Int): Long

    fun setLong(index: Int, value: Long): LongArrayNBTTag

    fun asLongIterable(): Iterable<Long>
}

interface ByteArrayNBTTag : CollectionNBTTag {
    val byteArray: ByteArray

    fun getByte(index: Int): Byte

    fun setByte(index: Int, value: Byte): ByteArrayNBTTag

    fun asByteIterable(): Iterable<Byte>
}

interface FloatArrayNBTTag : CollectionNBTTag {
    val floatArray: FloatArray

    fun asFloatIterable(): Iterable<Float>
}

interface DoubleArrayNBTTag : CollectionNBTTag {
    val doubleArray: DoubleArray

    fun asDoubleIterable(): Iterable<Double>
}

interface ListNBTTag : CollectionNBTTag, Iterable<NBTTag> {
    operator fun get(index: Int): NBTTag

    fun <R> get(index: Int, mapper: NBTTag.() -> R): R {
        if (index >= size)
            throw IllegalStateException("Requires element at $index, list only has $size")
        return try {
            mapper(get(index))
        } catch (ex: IllegalStateException) {
            throw IllegalStateException("Invalid element at $index: ${ex.message}")
        }
    }

    interface Mutable : ListNBTTag, CollectionNBTTag {
        operator fun set(index: Int, value: NBTTag): Mutable

        fun set(index: Int, tagCreator: NBTTag.() -> NBTTag): Mutable

        fun add(index: Int, tag: NBTTag): Mutable

        fun add(index: Int, tagCreator: NBTTag.() -> NBTTag): Mutable

        fun add(tag: NBTTag): Mutable

        fun addOr(tag: NBTTag?): Mutable

        fun add(tagCreator: NBTTag.() -> NBTTag): Mutable

        fun addOr(tagCreator: NBTTag.() -> NBTTag?): Mutable

        fun removeAt(index: Int): Mutable
    }
}
