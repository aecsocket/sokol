package com.gitlab.aecsocket.sokol.core

import java.util.UUID

interface NBTTag {
    fun ofInt(value: Int): NumericNBTTag
    fun ofLong(value: Long): NumericNBTTag
    fun ofByte(value: Byte): NumericNBTTag
    fun ofShort(value: Short): NumericNBTTag
    fun ofFloat(value: Float): NumericNBTTag
    fun ofDouble(value: Double): NumericNBTTag
    fun ofString(value: String): StringNBTTag
    fun ofUUID(value: UUID): UUIDNBTTag
    fun ofCompound(): CompoundNBTTag.Mutable
    fun ofIntArray(values: IntArray): IntArrayNBTTag
    fun ofLongArray(values: LongArray): LongArrayNBTTag
    fun ofByteArray(values: ByteArray): ByteArrayNBTTag
    fun ofList(): ListNBTTag.Mutable
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

private fun missing(key: String, type: String): () -> Nothing = {
    throw IllegalStateException("Missing key '$key' of type $type")
}

interface CompoundNBTTag : NBTTag, Iterable<Pair<String, NBTTag>> {
    val size: Int
    val keys: Set<String>
    val map: Map<String, NBTTag>

    fun contains(key: String): Boolean

    operator fun get(key: String): NBTTag?

    fun intOr(key: String): Int? = (get(key) as? NumericNBTTag)?.int
    fun int(key: String, default: Int): Int = (get(key) as? NumericNBTTag)?.int ?: default
    fun int(key: String, default: () -> Int = missing(key, "int")): Int = intOr(key) ?: default()

    fun longOr(key: String): Long? = (get(key) as? NumericNBTTag)?.long
    fun long(key: String, default: Long): Long = (get(key) as? NumericNBTTag)?.long ?: default
    fun long(key: String, default: () -> Long = missing(key, "long")): Long = longOr(key) ?: default()

    fun byteOr(key: String): Byte? = (get(key) as? NumericNBTTag)?.byte
    fun byte(key: String, default: Byte): Byte = (get(key) as? NumericNBTTag)?.byte ?: default
    fun byte(key: String, default: () -> Byte = missing(key, "long")): Byte = byteOr(key) ?: default()

    fun shortOr(key: String): Short? = (get(key) as? NumericNBTTag)?.short
    fun short(key: String, default: Short): Short = (get(key) as? NumericNBTTag)?.short ?: default
    fun short(key: String, default: () -> Short = missing(key, "long")): Short = shortOr(key) ?: default()


    fun floatOr(key: String): Float? = (get(key) as? NumericNBTTag)?.float
    fun float(key: String, default: Float): Float = (get(key) as? NumericNBTTag)?.float ?: default
    fun float(key: String, default: () -> Float = missing(key, "float")): Float = floatOr(key) ?: default()

    fun doubleOr(key: String): Double? = (get(key) as? NumericNBTTag)?.double
    fun double(key: String, default: Double): Double = (get(key) as? NumericNBTTag)?.double ?: default
    fun double(key: String, default: () -> Double = missing(key, "double")): Double = doubleOr(key) ?: default()

    fun stringOr(key: String): String? = (get(key) as? StringNBTTag)?.string
    fun string(key: String, default: String): String = (get(key) as? StringNBTTag)?.string ?: default
    fun string(key: String, default: () -> String = missing(key, "string")): String = stringOr(key) ?: default()


    fun uuidOr(key: String): UUID? = (get(key) as? UUIDNBTTag)?.uuid
    fun uuid(key: String, default: UUID): UUID = (get(key) as? UUIDNBTTag)?.uuid ?: default
    fun uuid(key: String, default: () -> UUID = missing(key, "uuid")): UUID = uuidOr(key) ?: default()

    fun compound(key: String) =
        get(key) as? CompoundNBTTag ?: ofCompound()
    fun intArray(key: String) =
        (get(key) as? IntArrayNBTTag)?.intArray ?: intArrayOf()
    fun longArray(key: String) =
        (get(key) as? LongArrayNBTTag)?.longArray ?: longArrayOf()
    fun byteArray(key: String) =
        (get(key) as? ByteArrayNBTTag)?.byteArray ?: byteArrayOf()
    fun list(key: String) =
        get(key) as? ListNBTTag ?: ofList()

    interface Mutable : CompoundNBTTag {
        operator fun set(key: String, tag: NBTTag): Mutable

        fun set(key: String, tagCreator: NBTTag.() -> NBTTag): Mutable

        fun remove(key: String): Mutable

        fun clear(): Mutable
    }
}

interface CollectionNBTTag<E> : NBTTag, Iterable<E> {
    val size: Int

    operator fun get(index: Int): E

    interface Mutable<E : NBTTag> : CollectionNBTTag<E> {
        operator fun set(index: Int, value: E): Mutable<E>

        fun add(index: Int, tag: E): Mutable<E>

        fun add(tag: E): Mutable<E>


        fun removeAt(index: Int): Mutable<E>
    }
}

interface IntArrayNBTTag : CollectionNBTTag<Int> {
    val intArray: IntArray
}

interface LongArrayNBTTag : CollectionNBTTag<Long> {
    val longArray: LongArray
}

interface ByteArrayNBTTag : CollectionNBTTag<Byte> {
    val byteArray: ByteArray
}

interface ListNBTTag : CollectionNBTTag<NBTTag> {
    interface Mutable : ListNBTTag, CollectionNBTTag.Mutable<NBTTag> {
        fun add(index: Int, tagCreator: NBTTag.() -> NBTTag): Mutable


        fun add(tagCreator: NBTTag.() -> NBTTag): Mutable
    }
}
