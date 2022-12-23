package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class Stats(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("stats")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Stats::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val stats: Map<StatKey<*>, Any>
    ) : SimpleComponentProfile {
        override val componentType get() = Stats::class

        override fun createEmpty() = ComponentBlueprint { Stats(this) }
    }
}

interface StatKey<T> {
    val key: Key
}

private class StatKeyImpl<T>(override val key: Key) : StatKey<T>

fun <T> statKeyOf(key: Key): StatKey<T> = StatKeyImpl(key)
