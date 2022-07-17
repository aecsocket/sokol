package com.gitlab.aecsocket.sokol.core.nbt

import java.util.UUID

class TagSerializationException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

interface BinaryTag {
    interface Scoped<T : Scoped<T>> {
        val self: T
    }
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

interface CollectionBinaryTag<E : BinaryTag> : BinaryTag, Iterable<E> {
    val size: Int

    fun get(index: Int): E

    interface Mutable<E : BinaryTag> : CollectionBinaryTag<E> {
        fun set(index: Int, value: E)

        fun add(index: Int, value: E)

        fun remove(index: Int)
    }
}

interface ListBinaryTag : BinaryTag, Iterable<BinaryTag> {
    val size: Int

    /*
        @Override
    public abstract T set(int i, T tag);

    @Override
    public abstract void add(int i, T tag);

    @Override
    public abstract T remove(int i);

    public abstract boolean setTag(int index, Tag element);

    public abstract boolean addTag(int index, Tag element);
     */
}

interface CompoundBinaryTag : BinaryTag, Iterable<Pair<String, BinaryTag>> {
    val size: Int

    fun has(key: String): Boolean
    operator fun get(key: String): BinaryTag?

    fun getByte(key: String, default: () -> Byte): Byte
    fun forceByte(key: String) = getByte(key) { throw TagSerializationException("No byte tag for key '$key'") }

    fun getShort(key: String, default: () -> Short): Short
    fun forceShort(key: String) = getShort(key) { throw TagSerializationException("No short tag for key '$key'") }

    fun getInt(key: String, default: () -> Int): Int
    fun forceInt(key: String) = getInt(key) { throw TagSerializationException("No int tag for key '$key'") }

    fun getLong(key: String, default: () -> Long): Long
    fun forceLong(key: String) = getLong(key) { throw TagSerializationException("No long tag for key '$key'") }

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

    interface Mutable : CompoundBinaryTag {
        operator fun set(key: String, tag: BinaryTag)
        fun setBoolean(key: String, value: Boolean)
        fun setByte(key: String, value: Byte)
        fun setShort(key: String, value: Short)
        fun setInt(key: String, value: Int)
        fun setLong(key: String, value: Long)
        fun setFloat(key: String, value: Float)
        fun setDouble(key: String, value: Double)
        fun setString(key: String, value: String)
        fun setUuid(key: String, value: UUID) // todo

        override fun getCompound(key: String): Mutable?
        fun newCompound(key: String): Mutable
        fun editCompound(key: String): Mutable

        fun remove(key: String)
    }
}
