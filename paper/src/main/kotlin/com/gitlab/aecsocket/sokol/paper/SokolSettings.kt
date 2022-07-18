package com.gitlab.aecsocket.sokol.paper

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SokolSettings(
    val enableBstats: Boolean = true,
    val hostResolution: HostResolver.Settings = HostResolver.Settings(),
    val nodeRenders: DefaultNodeRenders.Settings = DefaultNodeRenders.Settings(),
)
