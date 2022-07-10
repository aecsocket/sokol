package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.effect.ParticleEffect
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SokolSettings(
    val hostResolution: HostResolver.Settings = HostResolver.Settings(),
    val nodeRenders: NodeRenders.Settings = NodeRenders.Settings(),
)
