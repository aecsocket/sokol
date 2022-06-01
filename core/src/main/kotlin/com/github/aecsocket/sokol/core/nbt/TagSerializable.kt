package com.github.aecsocket.sokol.core.nbt

interface TagSerializable {
    fun serialize(tag: CompoundBinaryTag.Mutable)
}
