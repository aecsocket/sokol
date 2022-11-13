package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ItemEvent
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import net.kyori.adventure.key.Key
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import kotlin.reflect.KClass

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
        override fun createEmpty() = AsItem(this)
    }
}
