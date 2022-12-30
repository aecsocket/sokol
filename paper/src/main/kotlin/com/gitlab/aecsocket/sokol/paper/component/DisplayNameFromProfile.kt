package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class DisplayNameFromProfile(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("display_name_from_profile")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = DisplayNameFromProfile::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val prefix: String = "",
        val suffix: String = ""
    ) : SimpleComponentProfile {
        override val componentType get() = DisplayNameFromProfile::class

        override fun createEmpty() = ComponentBlueprint { DisplayNameFromProfile(this) }
    }
}

@All(DisplayNameFromProfile::class, Profiled::class)
@None(DisplayName::class)
@Before(DisplayNameTarget::class)
class DisplayNameFromProfileSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mDisplayNameFromProfile = ids.mapper<DisplayNameFromProfile>()
    private val mProfiled = ids.mapper<Profiled>()
    private val mDisplayName = ids.mapper<DisplayName>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val displayNameFromProfile = mDisplayNameFromProfile.get(entity).profile
        val profile = mProfiled.get(entity).profile

        mDisplayName.set(entity, DisplayName(displayNameFromProfile.prefix + profile.id + displayNameFromProfile.suffix))
    }
}
