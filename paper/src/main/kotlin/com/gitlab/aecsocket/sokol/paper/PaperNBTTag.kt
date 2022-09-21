package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.mapping
import com.gitlab.aecsocket.sokol.core.*
import net.minecraft.nbt.*
import java.nio.ByteBuffer
import java.util.*

private fun bytesOf(values: FloatArray) = ByteBuffer.allocate(4 * values.size).apply {
    values.forEach { putFloat(it) }
}.array()

private fun bytesOf(values: DoubleArray) = ByteBuffer.allocate(8 * values.size).apply {
    values.forEach { putDouble(it) }
}.array()

private fun floatsOf(values: ByteArray) = ByteBuffer.wrap(values).run {
    (0 until values.size / 4).map { float }.toFloatArray()
}

private fun doublesOf(values: ByteArray) = ByteBuffer.wrap(values).run {
    (0 until values.size / 8).map { double }.toDoubleArray()
}

sealed class PaperNBTTag(
    open val backing: Tag
) : NBTTag {
    override fun makeInt(value: Int) = PaperNumericTag(IntTag.valueOf(value))
    override fun makeLong(value: Long) = PaperNumericTag(LongTag.valueOf(value))
    override fun makeByte(value: Byte) = PaperNumericTag(ByteTag.valueOf(value))
    override fun makeShort(value: Short) = PaperNumericTag(ShortTag.valueOf(value))
    override fun makeFloat(value: Float) = PaperNumericTag(FloatTag.valueOf(value))
    override fun makeDouble(value: Double) = PaperNumericTag(DoubleTag.valueOf(value))

    override fun makeString(value: String) = PaperStringTag(StringTag.valueOf(value))

    override fun makeUUID(value: UUID) = PaperIntArrayTag(NbtUtils.createUUID(value))

    override fun makeCompound() = PaperCompoundTag(CompoundTag())

    override fun makeIntArray(values: IntArray) = PaperIntArrayTag(IntArrayTag(values))
    override fun makeLongArray(values: LongArray) = PaperLongArrayTag(LongArrayTag(values))
    override fun makeByteArray(values: ByteArray) = PaperByteArrayTag(ByteArrayTag(values))
    override fun makeFloatArray(values: FloatArray) = PaperByteArrayTag(ByteArrayTag(bytesOf(values)))
    override fun makeDoubleArray(values: DoubleArray) = PaperByteArrayTag(ByteArrayTag(bytesOf(values)))
    override fun makeList() = PaperListTag(ListTag())
}

private val NBTTag.backing get() = (this as PaperNBTTag).backing

data class PaperNumericTag(override val backing: NumericTag) : PaperNBTTag(backing), NumericNBTTag {
    override val int get() = backing.asInt
    override val long get() = backing.asLong
    override val byte get() = backing.asByte
    override val short get() = backing.asShort
    override val float get() = backing.asFloat
    override val double get() = backing.asDouble
}

data class PaperStringTag(override val backing: StringTag) : PaperNBTTag(backing), StringNBTTag {
    override val string: String get() = backing.asString
}

data class PaperCompoundTag(override val backing: CompoundTag) : PaperNBTTag(backing), CompoundNBTTag.Mutable {
    override val size get() = backing.size()
    override val keys: Set<String> get() = backing.allKeys
    override val map get() = backing.tags
        .map { (key, tag) -> key to paperTagOf(tag) }
        .associate { it }

    override fun contains(key: String) = backing.contains(key)

    override fun get(key: String) = backing.tags[key]?.let { paperTagOf(it) }

    override fun set(key: String, tag: NBTTag): PaperCompoundTag {
        backing.tags[key] = tag.backing
        return this
    }

    override fun set(key: String, tagCreator: NBTTag.() -> NBTTag): PaperCompoundTag {
        backing.tags[key] = tagCreator(this).backing
        return this
    }

    override fun setOrClear(key: String, tagCreator: NBTTag.() -> NBTTag?): PaperCompoundTag {
        tagCreator(this)?.let { set(key, it) } ?: remove(key)
        return this
    }

    override fun remove(key: String): PaperCompoundTag {
        backing.tags.remove(key)
        return this
    }

    override fun clear(): PaperCompoundTag {
        backing.tags.clear()
        return this
    }

    override fun iterator() = backing.tags.iterator().mapping { (key, tag) -> key to paperTagOf(tag) }
}

data class PaperIntArrayTag(override val backing: IntArrayTag) : PaperNBTTag(backing), IntArrayNBTTag, UUIDNBTTag {
    override val uuid: UUID get() = NbtUtils.loadUUID(backing)
    override val intArray: IntArray get() = backing.asIntArray

    override val size get() = backing.size

    override fun getInt(index: Int) = backing[index].asInt

    override fun setInt(index: Int, value: Int): PaperIntArrayTag {
        backing[index] = IntTag.valueOf(value)
        return this
    }

    override fun asIntIterable() = Iterable { backing.iterator().mapping { it.asInt } }
}

data class PaperLongArrayTag(override val backing: LongArrayTag) : PaperNBTTag(backing), LongArrayNBTTag {
    override val longArray: LongArray get() = backing.asLongArray

    override val size get() = backing.size

    override fun getLong(index: Int) = backing[index].asLong

    override fun setLong(index: Int, value: Long): PaperLongArrayTag {
        backing[index] = LongTag.valueOf(value)
        return this
    }

    override fun asLongIterable() = Iterable { backing.iterator().mapping { it.asLong } }
}

data class PaperByteArrayTag(override val backing: ByteArrayTag) : PaperNBTTag(backing), ByteArrayNBTTag, FloatArrayNBTTag, DoubleArrayNBTTag {
    override val byteArray: ByteArray get() = backing.asByteArray
    override val floatArray get() = floatsOf(backing.asByteArray)
    override val doubleArray get() = doublesOf(backing.asByteArray)

    override val size get() = backing.size

    override fun getByte(index: Int) = backing[index].asByte

    override fun setByte(index: Int, value: Byte): ByteArrayNBTTag {
        backing[index] = ByteTag.valueOf(value)
        return this
    }

    override fun asByteIterable() = Iterable { backing.iterator().mapping { it.asByte } }

    override fun asFloatIterable() = floatArray.asIterable()

    override fun asDoubleIterable() = doubleArray.asIterable()
}

data class PaperListTag(override val backing: ListTag) : PaperNBTTag(backing), ListNBTTag.Mutable {
    override val size get() = backing.size

    override fun get(index: Int) = paperTagOf(backing[index])

    override fun set(index: Int, value: NBTTag): PaperListTag {
        backing[index] = value.backing
        return this
    }

    override fun set(index: Int, tagCreator: NBTTag.() -> NBTTag): PaperListTag {
        backing[index] = tagCreator(this).backing
        return this
    }

    override fun add(index: Int, tag: NBTTag): PaperListTag {
        backing.add(index, tag.backing)
        return this
    }

    override fun add(index: Int, tagCreator: NBTTag.() -> NBTTag): PaperListTag {
        backing.add(index, tagCreator(this).backing)
        return this
    }

    override fun add(tag: NBTTag): PaperListTag {
        backing.add(tag.backing)
        return this
    }

    override fun add(tagCreator: NBTTag.() -> NBTTag): PaperListTag {
        backing.add(tagCreator(this).backing)
        return this
    }

    override fun removeAt(index: Int): PaperListTag {
        backing.removeAt(index)
        return this
    }

    override fun iterator() = backing.iterator().mapping { paperTagOf(it) }
}

fun paperTagOf(nms: Tag): PaperNBTTag {
    return when (nms) {
        is NumericTag -> PaperNumericTag(nms)
        is StringTag -> PaperStringTag(nms)
        is CompoundTag -> PaperCompoundTag(nms)
        is IntArrayTag -> PaperIntArrayTag(nms)
        is LongArrayTag -> PaperLongArrayTag(nms)
        is ByteArrayTag -> PaperByteArrayTag(nms)
        is ListTag -> PaperListTag(nms)
        else -> throw IllegalArgumentException("Invalid tag type ${nms.javaClass}")
    }
}
