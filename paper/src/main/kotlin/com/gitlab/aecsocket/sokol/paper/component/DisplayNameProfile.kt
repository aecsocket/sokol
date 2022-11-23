package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class DisplayNameProfile(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("display_name_profile")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = DisplayNameProfile::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val prefix: String = "",
        val suffix: String = "",
    ) : SimpleComponentProfile {
        override val componentType get() = DisplayNameProfile::class

        override fun createEmpty() = ComponentBlueprint { DisplayNameProfile(this) }
    }
}

@All(DisplayNameProfile::class, Profiled::class)
@None(DisplayName::class)
@Before(DisplayNameTarget::class)
class DisplayNameProfileSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mDisplayNameProfile = ids.mapper<DisplayNameProfile>()
    private val mProfiled = ids.mapper<Profiled>()
    private val mDisplayName = ids.mapper<DisplayName>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val displayNameProfile = mDisplayNameProfile.get(entity).profile
        val profile = mProfiled.get(entity).profile

        mDisplayName.set(entity, DisplayName(displayNameProfile.prefix + profile.id + displayNameProfile.suffix))
    }
}
