package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class ItemLoreContainerMap(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_container_map")
        val Type = ComponentType.deserializing(Key, Profile::class)
    }

    override val componentType get() = ItemLoreContainerMap::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val a: Boolean = true
    ) : SimpleComponentProfile<ItemLoreContainerMap> {
        override val componentType get() = ItemLoreContainerMap::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreContainerMap(this) }
    }
}
