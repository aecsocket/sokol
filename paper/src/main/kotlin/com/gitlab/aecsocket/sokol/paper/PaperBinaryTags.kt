package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.nbt.*
import net.minecraft.nbt.*
import java.util.*

internal fun wrap(handle: Tag) = when (handle) {
    is NumericTag -> PaperNumericTag(handle)
    is StringTag -> PaperStringTag(handle)
    is ByteArrayTag -> PaperByteArrayTag(handle)
    is IntArrayTag -> PaperIntArrayTag(handle)
    is LongArrayTag -> PaperLongArrayTag(handle)
    is ListTag -> PaperListTag(handle)
    is CompoundTag -> PaperCompoundTag(handle)
    else -> throw IllegalArgumentException("Tag of type ${handle.javaClass}")
}

internal abstract class PaperBinaryTag(
    open val handle: Tag
) : BinaryTag {
    override fun toString() = handle.toString()
    override fun newByte(value: Byte) = PaperNumericTag(ByteTag.valueOf(value))
    override fun newShort(value: Short) = PaperNumericTag(ShortTag.valueOf(value))
    override fun newInt(value: Int) = PaperNumericTag(IntTag.valueOf(value))
    override fun newLong(value: Long) = PaperNumericTag(LongTag.valueOf(value))
    override fun newFloat(value: Float) = PaperNumericTag(FloatTag.valueOf(value))
    override fun newDouble(value: Double) = PaperNumericTag(DoubleTag.valueOf(value))
    override fun newString(value: String) = PaperStringTag(StringTag.valueOf(value))
    override fun newByteArray(value: ByteArray) = PaperByteArrayTag(ByteArrayTag(value))
    override fun newIntArray(value: IntArray) = PaperIntArrayTag(IntArrayTag(value))
    override fun newLongArray(value: LongArray) = PaperLongArrayTag(LongArrayTag(value))
    override fun newList() = PaperListTag(ListTag())
    override fun newCompound() = PaperCompoundTag(CompoundTag())
}

internal class PaperNumericTag(
    override val handle: NumericTag
) : PaperBinaryTag(handle), NumericBinaryTag {
    override val asByte get() = handle.asByte
    override val asShort get() = handle.asShort
    override val asInt get() = handle.asInt
    override val asLong get() = handle.asLong
    override val asFloat get() = handle.asFloat
    override val asDouble get() = handle.asDouble
}

internal class PaperStringTag(
    override val handle: StringTag
) : PaperBinaryTag(handle), StringBinaryTag {
    override val value: String
        get() = handle.asString
}

internal class PaperByteArrayTag(
    override val handle: ByteArrayTag
) : PaperBinaryTag(handle), ByteArrayBinaryTag.Mutable {
    override val size get() = handle.size
    override val byteArray: ByteArray get() = handle.asByteArray

    override fun get(index: Int) = handle[index].asByte
    override fun set(index: Int, value: Byte) { handle[index] = ByteTag.valueOf(value) }
    override fun add(index: Int, value: Byte) { handle.add(index, ByteTag.valueOf(value)) }
    override fun add(value: Byte) { handle.add(ByteTag.valueOf(value)) }
    override fun remove(index: Int) = handle.removeAt(index).asByte

    override fun iterator(): MutableIterator<Byte> {
        val iter = handle.iterator()
        return object : MutableIterator<Byte> {
            override fun hasNext() = iter.hasNext()
            override fun next() = iter.next().asByte
            override fun remove() = iter.remove()
        }
    }
}

internal class PaperIntArrayTag(
    override val handle: IntArrayTag
) : PaperBinaryTag(handle), IntArrayBinaryTag.Mutable {
    override val size get() = handle.size
    override val intArray: IntArray get() = handle.asIntArray

    override fun get(index: Int) = handle[index].asInt
    override fun set(index: Int, value: Int) { handle[index] = IntTag.valueOf(value) }
    override fun add(index: Int, value: Int) { handle.add(index, IntTag.valueOf(value)) }
    override fun add(value: Int) { handle.add(IntTag.valueOf(value)) }
    override fun remove(index: Int) = handle.removeAt(index).asInt

    override fun iterator(): MutableIterator<Int> {
        val iter = handle.iterator()
        return object : MutableIterator<Int> {
            override fun hasNext() = iter.hasNext()
            override fun next() = iter.next().asInt
            override fun remove() = iter.remove()
        }
    }
}

internal class PaperLongArrayTag(
    override val handle: LongArrayTag
) : PaperBinaryTag(handle), LongArrayBinaryTag.Mutable {
    override val size get() = handle.size
    override val longArray: LongArray get() = handle.asLongArray

    override fun get(index: Int) = handle[index].asLong
    override fun set(index: Int, value: Long) { handle[index] = LongTag.valueOf(value) }
    override fun add(index: Int, value: Long) { handle.add(index, LongTag.valueOf(value)) }
    override fun add(value: Long) { handle.add(LongTag.valueOf(value)) }
    override fun remove(index: Int) = handle.removeAt(index).asLong

    override fun iterator(): MutableIterator<Long> {
        val iter = handle.iterator()
        return object : MutableIterator<Long> {
            override fun hasNext() = iter.hasNext()
            override fun next() = iter.next().asLong
            override fun remove() = iter.remove()
        }
    }
}

internal class PaperListTag(
    override val handle: ListTag
) : PaperBinaryTag(handle), ListBinaryTag.Mutable {
    override val size get() = handle.size

    override fun get(index: Int) = wrap(handle[index])
    override fun set(index: Int, value: BinaryTag) { handle[index] = (value as PaperBinaryTag).handle }
    override fun add(index: Int, value: BinaryTag) { handle.add(index, (value as PaperBinaryTag).handle) }
    override fun add(value: BinaryTag) { handle.add((value as PaperBinaryTag).handle) }
    override fun remove(index: Int) = wrap(handle.removeAt(index))

    override fun iterator(): MutableIterator<BinaryTag> {
        val iter = handle.iterator()
        return object : MutableIterator<BinaryTag> {
            override fun hasNext() = iter.hasNext()
            override fun next() = wrap(iter.next())
            override fun remove() = iter.remove()
        }
    }
}

internal class PaperCompoundTag(
    override val handle: CompoundTag
) : PaperBinaryTag(handle), CompoundBinaryTag.Mutable {
    override val size get() = handle.size()

    override fun iterator() = object : Iterator<Pair<String, BinaryTag>> {
        val nmsIterator = handle.tags.iterator()

        override fun hasNext() = nmsIterator.hasNext()

        override fun next(): Pair<String, BinaryTag> {
            val (key, tag) = nmsIterator.next()
            return key to wrap(tag)
        }
    }

    override fun has(key: String) = handle.contains(key)

    override fun get(key: String) = handle.get(key)?.let { wrap(it) }

    override fun set(key: String, tag: BinaryTag) {
        handle.put(key, (tag as PaperBinaryTag).handle)
    }

    override fun setBoolean(key: String, value: Boolean) {
        handle.put(key, ByteTag.valueOf(value))
    }

    private inline fun <T> getNumber(key: String, default: () -> T, get: (NumericTag) -> T) =
        handle.get(key)?.let { get(it as NumericTag) } ?: default()

    override fun getByte(key: String, default: () -> Byte) = getNumber(key, default) { it.asByte }
    override fun setByte(key: String, value: Byte) = handle.putByte(key, value)

    override fun getByteArray(key: String, default: () -> ByteArray) =
        handle.get(key)?.let { (it as ByteArrayTag).asByteArray } ?: default()
    override fun setByteArray(key: String, value: ByteArray) = handle.putByteArray(key, value)

    override fun getShort(key: String, default: () -> Short) = getNumber(key, default) { it.asShort }
    override fun setShort(key: String, value: Short) = handle.putShort(key, value)

    override fun getInt(key: String, default: () -> Int) = getNumber(key, default) { it.asInt }
    override fun setInt(key: String, value: Int) = handle.putInt(key, value)

    override fun getIntArray(key: String, default: () -> IntArray) =
        handle.get(key)?.let { (it as IntArrayTag).asIntArray } ?: default()
    override fun setIntArray(key: String, value: IntArray) = handle.putIntArray(key, value)

    override fun getLong(key: String, default: () -> Long) = getNumber(key, default) { it.asLong }
    override fun setLong(key: String, value: Long) = handle.putLong(key, value)

    override fun getLongArray(key: String, default: () -> LongArray) =
        handle.get(key)?.let { (it as LongArrayTag).asLongArray } ?: default()
    override fun setLongArray(key: String, value: LongArray) = handle.putLongArray(key, value)

    override fun getFloat(key: String, default: () -> Float) = getNumber(key, default) { it.asFloat }
    override fun setFloat(key: String, value: Float) = handle.putFloat(key, value)

    override fun getDouble(key: String, default: () -> Double) = getNumber(key, default) { it.asDouble }
    override fun setDouble(key: String, value: Double) = handle.putDouble(key, value)

    override fun getString(key: String) = handle.get(key)?.let { (it as StringTag).asString }
    override fun setString(key: String, value: String) = handle.putString(key, value)

    override fun getUuid(key: String) = handle.get(key)?.let { NbtUtils.loadUUID(it) }
    override fun setUuid(key: String, value: UUID) { handle.put(key, NbtUtils.createUUID(value)) }

    override fun getCompound(key: String) = handle.get(key)?.let { PaperCompoundTag(it as CompoundTag) }
    override fun newCompound(key: String) = PaperCompoundTag(CompoundTag()).also { handle.put(key, it.handle) }
    override fun editCompound(key: String) = handle.get(key)?.let {
        PaperCompoundTag(it as CompoundTag)
    } ?: newCompound(key)

    override fun getList(key: String) = handle.get(key)?.let { PaperListTag(it as ListTag) }
    override fun newList(key: String) = PaperListTag(ListTag()).also { handle.put(key, it.handle) }
    override fun editList(key: String) = handle.get(key)?.let {
        PaperListTag(it as ListTag)
    } ?: newList(key)

    override fun remove(key: String) = handle.remove(key)
}
