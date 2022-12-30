package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

interface StatType<T> {
    val key: Key

    data class OfFloat(override val key: Key) : StatType<Float>
}

class StatValue<T>

data class Stats(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("stats")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Stats::class
    override val key get() = Key

    private val statTypes = HashMap<Key, StatType<*>>()

    fun <T> statType(statType: StatType<T>) {
        statTypes[statType.key] = statType
    }

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val stats: Map<Key, Any>
    ) : SimpleComponentProfile {
        override val componentType get() = Stats::class

        override fun createEmpty() = ComponentBlueprint { Stats(this) }
    }
}

data class StatsInstance(
    val stats: Map<Key, StatValue<*>>
) : SokolComponent {
    override val componentType get() = StatsInstance::class
}

object StatsInstanceTarget : SokolSystem

@All(Stats::class)
@None(StatsInstance::class)
@Before(StatsInstanceTarget::class)
class StatsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mStats = ids.mapper<Stats>()

    object BuildStats : SokolEvent

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        entity.call(BuildStats)
    }

    @Subscribe
    fun on(event: BuildStats, entity: SokolEntity) {
        //val stats =
    }
}
