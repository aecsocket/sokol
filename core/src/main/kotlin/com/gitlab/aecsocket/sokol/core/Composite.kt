package com.gitlab.aecsocket.sokol.core

data class Composite(
    val children: List<SokolEntity>
) : SokolComponent, List<SokolEntity> by children {
    override val componentType get() = Composite::class
}
