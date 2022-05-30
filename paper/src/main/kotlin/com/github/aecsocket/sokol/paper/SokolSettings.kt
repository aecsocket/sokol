package com.github.aecsocket.sokol.paper

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SokolSettings(
    val temp: Int = 5
)
