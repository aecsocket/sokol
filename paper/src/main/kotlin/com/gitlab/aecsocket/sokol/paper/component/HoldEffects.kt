package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.SoundEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import kotlin.reflect.KClass

data class HoldEffects(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hold_effects")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { HoldEffectsSystem(it) }
        }
    }

    override val componentType get() = HoldEffects::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val startSound: SoundEngineEffect = SoundEngineEffect.Empty,
        val stopSound: SoundEngineEffect = SoundEngineEffect.Empty
    ) : SimpleComponentProfile<HoldEffects> {
        override val componentType get() = HoldEffects::class

        override fun createEmpty() = ComponentBlueprint { HoldEffects(this) }
    }
}

@All(HoldEffects::class, PositionAccess::class, Held::class)
class HoldEffectsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHoldEffects = ids.mapper<HoldEffects>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val holdEffects = mHoldEffects.get(entity).profile
        val location = mPositionAccess.get(entity).location()

        if (event.held) {
            AlexandriaAPI.sounds.play(holdEffects.startSound, location)
        } else {
            AlexandriaAPI.sounds.play(holdEffects.stopSound, location)
        }
    }
}
