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

    override fun remove(key: String) = handle.remove(key)
}
