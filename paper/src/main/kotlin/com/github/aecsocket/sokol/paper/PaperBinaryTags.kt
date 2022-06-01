package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.nbt.BinaryTag
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.nbt.NumericBinaryTag
import com.github.aecsocket.sokol.core.nbt.StringBinaryTag
import net.minecraft.nbt.*
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.collections.HashMap

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
