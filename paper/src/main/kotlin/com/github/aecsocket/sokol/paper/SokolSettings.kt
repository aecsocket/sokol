package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.effect.ParticleEffect
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SokolSettings(
    val hostResolution: HostResolution = HostResolution(),
    val inspectView: InspectView = InspectView(),
) {
    @ConfigSerializable
    data class HostResolution(
        val enabled: Boolean = true,
        val containerItems: Boolean = true,
        val containerBlocks: Boolean = true
    )

    @ConfigSerializable
    data class InspectView(
        val pointParticle: ParticleEffect? = null,
        val shapeParticle: ParticleEffect? = null,
        val shapeStep: Double = 0.2,
    )
}
