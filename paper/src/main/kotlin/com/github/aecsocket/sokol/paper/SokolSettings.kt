package com.github.aecsocket.sokol.paper

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SokolSettings(
    val hostResolution: HostResolution = HostResolution()
) {
    @ConfigSerializable
    data class HostResolution(
        val enabled: Boolean = true,
        val containerItems: Boolean = true,
        val containerBlocks: Boolean = true
    )
}
