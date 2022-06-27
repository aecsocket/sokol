package com.github.aecsocket.sokol.core.stat

import net.kyori.adventure.key.Key

class DecimalStat(
    namespace: String,
    key: String
) : Stat<Double> {
    override val key = Key.key(namespace, key)
}
