package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.nbt.BinaryTag
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.nbt.NumericBinaryTag
import com.github.aecsocket.sokol.core.nbt.StringBinaryTag
import net.minecraft.nbt.*
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import java.util.*

internal fun fromInternal(handle: Tag) = when (handle) {
    is NumericTag -> PaperNumericTag(handle)
    is StringTag -> PaperStringTag(handle)
    is CompoundTag -> PaperCompoundTag(handle)
    else -> TODO("add more types")
}

internal abstract class PaperBinaryTag(
    open val handle: Tag
) : BinaryTag {
    override fun toString() = handle.toString()
}

internal class PaperNumericTag(
    override val handle: NumericTag
) : PaperBinaryTag(handle), NumericBinaryTag {
    override val asByte: Byte
        get() = handle.asByte
    override val asShort: Short
        get() = handle.asShort
    override val asInt: Int
        get() = handle.asInt
    override val asLong: Long
        get() = handle.asLong
    override val asFloat: Float
        get() = handle.asFloat
    override val asDouble: Double
        get() = handle.asDouble
}

internal class PaperStringTag(
    override val handle: StringTag
) : PaperBinaryTag(handle), StringBinaryTag {
    override val value: String
        get() = handle.asString
}

internal class PaperCompoundTag(
    override val handle: CompoundTag
) : PaperBinaryTag(handle), CompoundBinaryTag.Mutable {
    override val size: Int
        get() = handle.size()

    override fun iterator() = object : Iterator<Pair<String, BinaryTag>> {
        val nmsIterator = handle.tags.iterator()

        override fun hasNext() = nmsIterator.hasNext()

        override fun next(): Pair<String, BinaryTag> {
            val (key, tag) = nmsIterator.next()
            return key to fromInternal(tag)
        }
    }

    override fun has(key: String) = handle.contains(key)

    override fun get(key: String) = handle.get(key)?.let { fromInternal(it) }

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

    override fun getShort(key: String, default: () -> Short) = getNumber(key, default) { it.asShort }
    override fun setShort(key: String, value: Short) = handle.putShort(key, value)

    override fun getInt(key: String, default: () -> Int) = getNumber(key, default) { it.asInt }
    override fun setInt(key: String, value: Int) = handle.putInt(key, value)

    override fun getLong(key: String, default: () -> Long) = getNumber(key, default) { it.asLong }
    override fun setLong(key: String, value: Long) = handle.putLong(key, value)

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
}

/*
object PaperBinaryTags {
    fun fromStack(stack: ItemStack): CompoundBinaryTag.Mutable? {
        return if (stack is CraftItemStack) stack.handle.tag?.let {
            NMSCompoundTag(it)
        } else null
    }
}

internal fun fromNms(nms: Tag) = when (nms) {
    is NumericTag -> NMSNumericTag(nms)
    is StringTag -> NMSStringTag(nms)
    is CompoundTag -> NMSCompoundTag(nms)
    else -> TODO("todo")
}

internal abstract class NMSBinaryTag(
    open val nms: Tag
) : BinaryTag {
    override fun toString() = nms.toString()
}

internal class NMSNumericTag(
    override val nms: NumericTag
) : NMSBinaryTag(nms), NumericBinaryTag {
    override val asByte: Byte
        get() = nms.asByte
    override val asShort: Short
        get() = nms.asShort
    override val asInt: Int
        get() = nms.asInt
    override val asLong: Long
        get() = nms.asLong
    override val asFloat: Float
        get() = nms.asFloat
    override val asDouble: Double
        get() = nms.asDouble
}

internal class NMSStringTag(
    override val nms: StringTag
) : NMSBinaryTag(nms), StringBinaryTag {
    override val value: String
        get() = nms.asString
}

internal class NMSCompoundTag(
    override val nms: CompoundTag
) : NMSBinaryTag(nms), CompoundBinaryTag.Mutable {
    private val cache = HashMap<String, BinaryTag>()

    override val size: Int
        get() = nms.size()

    override fun iterator() = object : Iterator<Pair<String, BinaryTag>> {
        val nmsIter = nms.tags.iterator()

        override fun hasNext() = nmsIter.hasNext()

        override fun next(): Pair<String, BinaryTag> {
            val (key, nmsTag) = nmsIter.next()
            return key to cache.computeIfAbsent(key) { fromNms(nmsTag) }
        }
    }

    override fun has(key: String) = nms.contains(key)

    override fun get(key: String) = cache[key] ?: nms.get(key)?.let { fromNms(it) }?.also { cache[key] = it }

    override fun set(key: String, tag: BinaryTag) {
        cache[key] = tag
        nms.put(key, (tag as NMSBinaryTag).nms) // I know this isn't type safe. I don't care, it's internals.
    }

    private inline fun <N> getNumber(key: String, default: () -> N, get: (NMSNumericTag) -> N) =
        get(key)?.let { get(it as NMSNumericTag) } ?: default()

    private inline fun setNumber(key: String, create: () -> NumericTag): NumericBinaryTag {
        val tag = create()
        nms.put(key, tag)
        val ourTag = NMSNumericTag(tag)
        cache[key] = ourTag
        return ourTag
    }

    override fun setBoolean(key: String, value: Boolean) = setByte(key, 1)

    override fun getByte(key: String, default: () -> Byte) =
        getNumber(key, default) { it.asByte }
    override fun setByte(key: String, value: Byte) =
        setNumber(key) { ByteTag.valueOf(value) }

    override fun getShort(key: String, default: () -> Short) =
        getNumber(key, default) { it.asShort }
    override fun setShort(key: String, value: Short) =
        setNumber(key) { ShortTag.valueOf(value) }

    override fun getInt(key: String, default: () -> Int) =
        getNumber(key, default) { it.asInt }
    override fun setInt(key: String, value: Int) =
        setNumber(key) { IntTag.valueOf(value) }

    override fun getLong(key: String, default: () -> Long) =
        getNumber(key, default) { it.asLong }
    override fun setLong(key: String, value: Long) =
        setNumber(key) { LongTag.valueOf(value) }

    override fun getFloat(key: String, default: () -> Float) =
        getNumber(key, default) { it.asFloat }
    override fun setFloat(key: String, value: Float) =
        setNumber(key) { FloatTag.valueOf(value) }

    override fun getDouble(key: String, default: () -> Double) =
        getNumber(key, default) { it.asDouble }
    override fun setDouble(key: String, value: Double) =
        setNumber(key) { DoubleTag.valueOf(value) }

    override fun getString(key: String) =
        get(key)?.let { (it as NMSStringTag).value }
    override fun setString(key: String, value: String): StringBinaryTag {
        val tag = StringTag.valueOf(value)
        nms.put(key, tag)
        val ourTag = NMSStringTag(tag)
        cache[key] = ourTag
        return ourTag
    }

    // these methods bypass Paper legacy loading stuff
    // todo cache these too
    override fun getUuid(key: String) =
        nms.get(key)?.let { NbtUtils.loadUUID(it) }
    override fun setUuid(key: String, value: UUID) {
        nms.put(key, NbtUtils.createUUID(value))
    }

    override fun getCompound(key: String) =
        get(key)?.let { it as NMSCompoundTag }

    override fun newCompound(key: String): CompoundBinaryTag.Mutable {
        TODO("Not yet implemented")
    }

    override fun getOrEmpty(key: String): CompoundBinaryTag.Mutable {
        return cache.computeIfAbsent(key) {
            nms.get(key)?.let { NMSCompoundTag(it as CompoundTag) } ?: run {
                val tag = NMSCompoundTag(CompoundTag())
                nms.put(key, tag.nms)
                tag
            }
        } as NMSCompoundTag
    }

    override fun setCompound(key: String, content: CompoundBinaryTag.Mutable.() -> Unit): NMSCompoundTag {
        val tag = NMSCompoundTag(CompoundTag())
        content(tag)
        nms.put(key, tag.nms)
        cache[key] = tag
        return tag
    }
}
*/
