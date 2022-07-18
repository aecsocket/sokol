package com.gitlab.aecsocket.sokol.paper

enum class ServerMapping(internal val testClass: String) {
    MOJANG  ("net.minecraft.world.phys.AABB"),
    SPIGOT  ("net.minecraft.world.phys.AxisAlignedBB"),
}

val serverMapping: ServerMapping? by lazy {
    ServerMapping.values().firstOrNull {
        try {
            Class.forName(it.testClass)
            true
        } catch (ex: ClassNotFoundException) {
            false
        }
    }
}
