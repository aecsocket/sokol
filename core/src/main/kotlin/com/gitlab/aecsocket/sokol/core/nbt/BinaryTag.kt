package com.gitlab.aecsocket.sokol.core.nbt

import java.util.UUID

class TagSerializationException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

interface BinaryTag {
    fun newByte(value: Byte): NumericBinaryTag
    fun newShort(value: Short): NumericBinaryTag
    fun newInt(value: Int): NumericBinaryTag
    fun newLong(value: Long): NumericBinaryTag
    fun newFloat(value: Float): NumericBinaryTag
    fun newDouble(value: Double): NumericBinaryTag
    fun newString(value: String): StringBinaryTag
    fun newByteArray(value: ByteArray): ByteArrayBinaryTag.Mutable
    fun newIntArray(value: IntArray): IntArrayBinaryTag.Mutable
    fun newLongArray(value: LongArray): LongArrayBinaryTag.Mutable
    fun newList(): ListBinaryTag.Mutable
    fun newCompound(): CompoundBinaryTag.Mutable

    fun forceNumeric(): NumericBinaryTag = if (this is NumericBinaryTag) this
        else throw TagSerializationException("Must be numeric tag")
    fun forceString(): StringBinaryTag = if (this is StringBinaryTag) this
        else throw TagSerializationException("Must be string tag")
    fun forceByteArray(): ByteArrayBinaryTag = if (this is ByteArrayBinaryTag) this
        else throw TagSerializationException("Must be byte array tag")
    fun forceIntArray(): IntArrayBinaryTag = if (this is IntArrayBinaryTag) this
        else throw TagSerializationException("Must be int array tag")
    fun forceLongArray(): LongArrayBinaryTag = if (this is LongArrayBinaryTag) this
        else throw TagSerializationException("Must be long array tag")
    fun forceList(): ListBinaryTag = if (this is ListBinaryTag) this
        else throw TagSerializationException("Must be list tag")
    fun forceCompound(): CompoundBinaryTag = if (this is CompoundBinaryTag) this
        else throw TagSerializationException("Must be compound tag")
}

interface NumericBinaryTag : BinaryTag {
    val asByte: Byte
    val asShort: Short
    val asInt: Int
    val asLong: Long
    val asFloat: Float
    val asDouble: Double
}

interface StringBinaryTag : BinaryTag {
    val value: String
}

interface CollectionBinaryTag<E> : BinaryTag, Iterable<E> {
    val size: Int

    fun get(index: Int): E

    interface Mutable<E> : CollectionBinaryTag<E>, MutableIterable<E> {
        fun set(index: Int, value: E)

        fun add(index: Int, value: E)

        fun add(value: E)

        fun remove(index: Int): E
    }
}

interface ByteArrayBinaryTag : CollectionBinaryTag<Byte> {
    val byteArray: ByteArray

    interface Mutable : ByteArrayBinaryTag, CollectionBinaryTag.Mutable<Byte>
}

interface IntArrayBinaryTag : CollectionBinaryTag<Int> {
    val intArray: IntArray

    interface Mutable : IntArrayBinaryTag, CollectionBinaryTag.Mutable<Int>
}

interface LongArrayBinaryTag : CollectionBinaryTag<Long> {
    val longArray: LongArray

    interface Mutable : LongArrayBinaryTag, CollectionBinaryTag.Mutable<Long>
}

interface ListBinaryTag : CollectionBinaryTag<BinaryTag> {
    interface Mutable : ListBinaryTag, CollectionBinaryTag.Mutable<BinaryTag>
}

interface CompoundBinaryTag : BinaryTag, Iterable<Pair<String, BinaryTag>> {
    val size: Int

    fun has(key: String): Boolean
    operator fun get(key: String): BinaryTag?

    fun getByte(key: String, default: () -> Byte): Byte
    fun forceByte(key: String) = getByte(key) { throw TagSerializationException("No byte tag for key '$key'") }

    fun getByteArray(key: String, default: () -> ByteArray): ByteArray
    fun forceByteArray(key: String) = getByteArray(key) { throw TagSerializationException("No byte array tag for key '$key'") }

    fun getShort(key: String, default: () -> Short): Short
    fun forceShort(key: String) = getShort(key) { throw TagSerializationException("No short tag for key '$key'") }

    fun getInt(key: String, default: () -> Int): Int
    fun forceInt(key: String) = getInt(key) { throw TagSerializationException("No int tag for key '$key'") }

    fun getIntArray(key: String, default: () -> IntArray): IntArray
    fun forceIntArray(key: String) = getIntArray(key) { throw TagSerializationException("No int array tag for key '$key'") }

    fun getLong(key: String, default: () -> Long): Long
    fun forceLong(key: String) = getLong(key) { throw TagSerializationException("No long tag for key '$key'") }

    fun getLongArray(key: String, default: () -> LongArray): LongArray
    fun forceLongArray(key: String) = getLongArray(key) { throw TagSerializationException("No long array tag for key '$key'") }

    fun getFloat(key: String, default: () -> Float): Float
    fun forceFloat(key: String) = getFloat(key) { throw TagSerializationException("No float tag for key '$key'") }

    fun getDouble(key: String, default: () -> Double): Double
    fun forceDouble(key: String) = getShort(key) { throw TagSerializationException("No double tag for key '$key'") }

    fun getString(key: String): String?
    fun forceString(key: String) = getString(key) ?: throw TagSerializationException("No string tag for key '$key'")

    fun getUuid(key: String): UUID?
    fun forceUuid(key: String) = getUuid(key) ?: throw TagSerializationException("No UUID tag for key '$key'")

    fun getCompound(key: String): CompoundBinaryTag?
    fun forceCompound(key: String) = getCompound(key) ?: throw TagSerializationException("No compound tag for key '$key'")

    fun getList(key: String): ListBinaryTag?
    fun forceList(key: String) = getList(key) ?: throw TagSerializationException("No list tag for key '$key'")

    interface Mutable : CompoundBinaryTag {
        operator fun set(key: String, tag: BinaryTag)
        fun setBoolean(key: String, value: Boolean)
        fun setByte(key: String, value: Byte)
        fun setByteArray(key: String, value: ByteArray)
        fun setByteArray(key: String, value: Collection<Byte>): Unit = setByteArray(key, value.toByteArray())
        fun setShort(key: String, value: Short)
        fun setInt(key: String, value: Int)
        fun setIntArray(key: String, value: IntArray)
        fun setIntArray(key: String, value: Collection<Int>): Unit = setIntArray(key, value.toIntArray())
        fun setLong(key: String, value: Long)
        fun setLongArray(key: String, value: LongArray)
        fun setLongArray(key: String, value: Collection<Long>): Unit = setLongArray(key, value.toLongArray())
        fun setFloat(key: String, value: Float)
        fun setDouble(key: String, value: Double)
        fun setString(key: String, value: String)
        fun setUuid(key: String, value: UUID)

        override fun getCompound(key: String): Mutable?
        fun newCompound(key: String): Mutable
        fun editCompound(key: String): Mutable

        override fun getList(key: String): ListBinaryTag.Mutable?
        fun newList(key: String): ListBinaryTag.Mutable
        fun editList(key: String): ListBinaryTag.Mutable

        fun remove(key: String)
    }
}
