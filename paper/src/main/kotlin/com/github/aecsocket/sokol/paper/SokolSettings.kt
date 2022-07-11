package com.github.aecsocket.sokol.paper

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SokolSettings(
    val hostResolution: HostResolver.Settings = HostResolver.Settings(),
    val nodeRenders: DefaultNodeRenders.Settings = DefaultNodeRenders.Settings(),
)
