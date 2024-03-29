package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

object Hostable {
    fun init(ctx: Sokol.InitContext) {
        ctx.persistentComponent(AsMob.Type)
        ctx.persistentComponent(AsItem.Type)
    }
}

object AsMob : SimplePersistentComponent {
    override val componentType get() = AsMob::class
    override val key = SokolAPI.key("as_mob")
    val Type = ComponentType.singletonComponent(key, this)
}

data class AsItem(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("as_item")
        val Type = ComponentType.deserializing(Key, Profile::class)
    }

    override val componentType get() = AsItem::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val item: ItemDescriptor
    ) : SimpleComponentProfile<AsItem> {
        override val componentType get() = AsItem::class

        override fun createEmpty() = ComponentBlueprint { AsItem(this) }
    }
}
