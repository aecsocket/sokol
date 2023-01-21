package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.REPLACE_MARKER
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting

data class DisplayNameFromProfile(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("display_name_from_profile")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { DisplayNameFromProfileSystem(it) }
            ctx.system { DisplayNameFromProfileForwardSystem(it) }
        }
    }

    override val componentType get() = DisplayNameFromProfile::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required @Setting(nodeFromParent = true) val template: String
    ) : SimpleComponentProfile<DisplayNameFromProfile> {
        override val componentType get() = DisplayNameFromProfile::class

        override fun createEmpty() = ComponentBlueprint { DisplayNameFromProfile(this) }
    }
}

@All(DisplayNameFromProfile::class, Profiled::class)
@None(DisplayName::class)
class DisplayNameFromProfileSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mDisplayNameFromProfile = ids.mapper<DisplayNameFromProfile>()
    private val mProfiled = ids.mapper<Profiled>()
    private val mDisplayName = ids.mapper<DisplayName>()

    object Construct : SokolEvent

    @Subscribe
    fun on(event: Construct, entity: SokolEntity) {
        val displayNameFromProfile = mDisplayNameFromProfile.get(entity).profile
        val profile = mProfiled.get(entity).profile

        val key = displayNameFromProfile.template.replace(REPLACE_MARKER, profile.id)
        mDisplayName.set(entity, DisplayName(key))
    }
}

@Before(DisplayNameTarget::class)
class DisplayNameFromProfileForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, DisplayNameFromProfileSystem.Construct)
    }
}
