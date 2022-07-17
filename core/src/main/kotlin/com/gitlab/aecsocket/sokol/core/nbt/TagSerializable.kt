package com.gitlab.aecsocket.sokol.core.nbt

interface TagSerializable {
    fun serialize(tag: CompoundBinaryTag.Mutable)
}
