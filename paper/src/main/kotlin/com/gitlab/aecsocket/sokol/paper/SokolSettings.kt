package com.gitlab.aecsocket.sokol.paper

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

@ConfigSerializable
data class SokolSettings(
    val enableBstats: Boolean = true,
    @Required val hostFeatures: HostFeatures,
    val hostResolution: HostResolver.Settings = HostResolver.Settings(),
    val nodeRenders: DefaultNodeRenders.Settings = DefaultNodeRenders.Settings(),
) {
    @ConfigSerializable
    data class HostFeatures(
        @Required val item: FeatureRef,
    )
}
