package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.mapping
import com.gitlab.aecsocket.sokol.core.*
import net.minecraft.nbt.*
import java.util.*

sealed class PaperNBTTag(
    open val backing: Tag
) : NBTTag {
    override fun ofInt(value: Int) = PaperNumericTag(IntTag.valueOf(value))
    override fun ofLong(value: Long) = PaperNumericTag(LongTag.valueOf(value))
    override fun ofByte(value: Byte) = PaperNumericTag(ByteTag.valueOf(value))
    override fun ofShort(value: Short) = PaperNumericTag(ShortTag.valueOf(value))
    override fun ofFloat(value: Float) = PaperNumericTag(FloatTag.valueOf(value))
    override fun ofDouble(value: Double) = PaperNumericTag(DoubleTag.valueOf(value))

    override fun ofString(value: String) = PaperStringTag(StringTag.valueOf(value))

    override fun ofUUID(value: UUID) = PaperIntArrayTag(NbtUtils.createUUID(value))

    override fun ofCompound() = PaperCompoundTag(CompoundTag())

    override fun ofIntArray(values: IntArray) = PaperIntArrayTag(IntArrayTag(values))
    override fun ofLongArray(values: LongArray) = PaperLongArrayTag(LongArrayTag(values))
    override fun ofByteArray(values: ByteArray) = PaperByteArrayTag(ByteArrayTag(values))
    override fun ofList() = PaperListTag(ListTag())
}

private val NBTTag.backing get() = (this as PaperNBTTag).backing

class PaperNumericTag(override val backing: NumericTag) : PaperNBTTag(backing), NumericNBTTag {
    override val int get() = backing.asInt
    override val long get() = backing.asLong
    override val byte get() = backing.asByte
    override val short get() = backing.asShort
    override val float get() = backing.asFloat
    override val double get() = backing.asDouble
}

class PaperStringTag(override val backing: StringTag) : PaperNBTTag(backing), StringNBTTag {
    override val string: String get() = backing.asString
}

class PaperCompoundTag(override val backing: CompoundTag) : PaperNBTTag(backing), CompoundNBTTag.Mutable {
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

class PaperIntArrayTag(override val backing: IntArrayTag) : PaperNBTTag(backing), IntArrayNBTTag, UUIDNBTTag {
    override val uuid: UUID get() = NbtUtils.loadUUID(backing)
    override val intArray: IntArray get() = backing.asIntArray

    override val size get() = backing.size

    override fun get(index: Int) = backing[index].asInt

    override fun iterator() = backing.iterator().mapping { it.asInt }
}

class PaperLongArrayTag(override val backing: LongArrayTag) : PaperNBTTag(backing), LongArrayNBTTag {
    override val longArray: LongArray get() = backing.asLongArray

    override val size get() = backing.size

    override fun get(index: Int) = backing[index].asLong

    override fun iterator() = backing.iterator().mapping { it.asLong }
}

class PaperByteArrayTag(override val backing: ByteArrayTag) : PaperNBTTag(backing), ByteArrayNBTTag {
    override val byteArray: ByteArray get() = backing.asByteArray

    override val size get() = backing.size

    override fun get(index: Int) = backing[index].asByte

    override fun iterator() = backing.iterator().mapping { it.asByte }
}

class PaperListTag(override val backing: ListTag) : PaperNBTTag(backing), ListNBTTag.Mutable {
    override val size get() = backing.size

    override fun get(index: Int) = paperTagOf(backing[index])

    override fun set(index: Int, value: NBTTag): PaperListTag {
        backing[index] = value.backing
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
