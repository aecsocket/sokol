package com.gitlab.aecsocket.sokol.core

import java.util.UUID

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

private fun NBTTag.wrongType(expected: String): Nothing {
    throw PersistenceException("Expected tag of type $expected, was $typeName")
}

private const val TYPE_BOOLEAN = "boolean"
private const val TYPE_NUMERIC = "numeric"
private const val TYPE_STRING = "string"
private const val TYPE_UUID = "uuid"
private const val TYPE_COMPOUND = "compound"
private const val TYPE_INT_ARRAY = "int_array"
private const val TYPE_LONG_ARRAY = "long_array"
private const val TYPE_BYTE_ARRAY = "byte_array"
private const val TYPE_FLOAT_ARRAY = "float_array"
private const val TYPE_DOUBLE_ARRAY = "double_array"
private const val TYPE_LIST = "list"

interface NBTTag : NBTTagContext {
    val typeName: String

    fun asBoolean() = (this as? BooleanNBTTag)?.boolean ?: wrongType(TYPE_BOOLEAN)
    fun asInt() = (this as? NumericNBTTag)?.int ?: wrongType(TYPE_NUMERIC)
    fun asLong() = (this as? NumericNBTTag)?.long ?: wrongType(TYPE_NUMERIC)
    fun asByte() = (this as? NumericNBTTag)?.byte ?: wrongType(TYPE_NUMERIC)
    fun asShort() = (this as? NumericNBTTag)?.short ?: wrongType(TYPE_NUMERIC)
    fun asFloat() = (this as? NumericNBTTag)?.float ?: wrongType(TYPE_NUMERIC)
    fun asDouble() = (this as? NumericNBTTag)?.double ?: wrongType(TYPE_NUMERIC)
    fun asString() = (this as? StringNBTTag)?.string ?: wrongType(TYPE_STRING)
    fun asUUID() = (this as? UUIDNBTTag)?.uuid ?: wrongType(TYPE_UUID)
    fun asCompound() = this as? CompoundNBTTag ?: wrongType(TYPE_COMPOUND)
    fun asIntArray() = (this as? IntArrayNBTTag)?.intArray ?: wrongType(TYPE_INT_ARRAY)
    fun asLongArray() = (this as? LongArrayNBTTag)?.longArray ?: wrongType(TYPE_LONG_ARRAY)
    fun asByteArray() = (this as? ByteArrayNBTTag)?.byteArray ?: wrongType(TYPE_BYTE_ARRAY)
    fun asFloatArray() = (this as? FloatArrayNBTTag)?.floatArray ?: wrongType(TYPE_FLOAT_ARRAY)
    fun asDoubleArray() = (this as? DoubleArrayNBTTag)?.doubleArray ?: wrongType(TYPE_DOUBLE_ARRAY)
    fun asList() = this as? ListNBTTag ?: wrongType(TYPE_LIST)
}

interface BooleanNBTTag : NBTTag {
    override val typeName get() = TYPE_BOOLEAN

    val boolean: Boolean
}

interface NumericNBTTag : NBTTag {
    override val typeName get() = TYPE_NUMERIC

    val int: Int
    val long: Long
    val byte: Byte
    val short: Short
    val float: Float
    val double: Double
}

interface StringNBTTag : NBTTag {
    override val typeName get() = TYPE_STRING

    val string: String
}

interface UUIDNBTTag : NBTTag {
    override val typeName get() = TYPE_UUID

    val uuid: UUID
}

interface CompoundNBTTag : NBTTag, Iterable<Pair<String, NBTTag>> {
    override val typeName get() = TYPE_COMPOUND

    val size: Int
    val keys: Set<String>
    val map: Map<String, NBTTag>

    fun contains(key: String): Boolean

    operator fun get(key: String): NBTTag?

    fun <R> get(key: String, mapper: NBTTag.() -> R): R {
        val tag = get(key) ?: throw PersistenceException("Requires key '$key'")
        return try {
            mapper(tag)
        } catch (ex: PersistenceException) {
            throw PersistenceException("Invalid key '$key': ${ex.message}")
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
    override val typeName get() = TYPE_INT_ARRAY

    val intArray: IntArray

    fun getInt(index: Int): Int

    fun setInt(index: Int, value: Int): IntArrayNBTTag

    fun asIntIterable(): Iterable<Int>
}

interface LongArrayNBTTag : CollectionNBTTag {
    override val typeName get() = TYPE_LONG_ARRAY

    val longArray: LongArray

    fun getLong(index: Int): Long

    fun setLong(index: Int, value: Long): LongArrayNBTTag

    fun asLongIterable(): Iterable<Long>
}

interface ByteArrayNBTTag : CollectionNBTTag {
    override val typeName get() = TYPE_BYTE_ARRAY

    val byteArray: ByteArray

    fun getByte(index: Int): Byte

    fun setByte(index: Int, value: Byte): ByteArrayNBTTag

    fun asByteIterable(): Iterable<Byte>
}

interface FloatArrayNBTTag : CollectionNBTTag {
    override val typeName get() = TYPE_FLOAT_ARRAY

    val floatArray: FloatArray

    fun asFloatIterable(): Iterable<Float>
}

interface DoubleArrayNBTTag : CollectionNBTTag {
    override val typeName get() = TYPE_DOUBLE_ARRAY

    val doubleArray: DoubleArray

    fun asDoubleIterable(): Iterable<Double>
}

interface ListNBTTag : CollectionNBTTag, Iterable<NBTTag> {
    override val typeName get() = TYPE_LIST

    operator fun get(index: Int): NBTTag

    fun <R> get(index: Int, mapper: NBTTag.() -> R): R {
        if (index >= size)
            throw PersistenceException("Requires element at $index, list only has $size")
        return try {
            mapper(get(index))
        } catch (ex: PersistenceException) {
            throw PersistenceException("Invalid element at $index: ${ex.message}")
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

fun <R> NBTTag.asCompound(mapper: (CompoundNBTTag) -> R) = asCompound().run(mapper)

fun <R> CompoundNBTTag.getCompound(key: String, mapper: (CompoundNBTTag) -> R) = get(key) { asCompound().run(mapper) }

fun <R> CompoundNBTTag.getCompoundOr(key: String, mapper: (CompoundNBTTag) -> R?) = getOr(key) { asCompound().run(mapper) }

fun <R> Iterable<NBTTag>.mapCompound(mapper: (CompoundNBTTag) -> R) = map { mapper(it.asCompound()) }

fun <V> ListNBTTag.Mutable.from(
    values: Iterable<V>,
    mapper: NBTTagContext.(V) -> NBTTag?
): ListNBTTag.Mutable {
    values.forEach {
        mapper(it)?.let(this::add)
    }
    return this
}

fun <V> CompoundNBTTag.Mutable.setList(
    key: String,
    values: Iterable<V>,
    mapper: NBTTagContext.(V) -> NBTTag?
): CompoundNBTTag.Mutable {
    set(key) { makeList().from(values, mapper) }
    return this
}
