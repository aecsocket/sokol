package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

object AsMob : SimplePersistentComponent {
    override val componentType get() = AsMob::class
    override val key = SokolAPI.key("as_mob")
    val Type = ComponentType.singletonComponent(key, this)
}

data class AsItem(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("as_item")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = AsItem::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val item: ItemDescriptor
    ) : SimpleComponentProfile {
        override val componentType get() = AsItem::class

        override fun createEmpty(entity: SokolEntity, space: SokolSpace) = AsItem(this)
    }
}
