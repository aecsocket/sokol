package com.github.aecsocket.sokol.core.stat

import net.kyori.adventure.key.Key

interface Stat<T : Any> {
    val key: Key
}
