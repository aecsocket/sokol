package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class HostableByItem(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("hostable_by_item")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HostableByItem::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val descriptor: ItemDescriptor,
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = HostableByItem(this)

        override fun read(node: ConfigurationNode) = HostableByItem(this)
    }
}

object HostableByMob : PersistentComponent {
    override val componentType get() = HostableByMob::class
    override val key = SokolAPI.key("hostable_by_mob")
    val Type = ComponentType.singletonComponent(key, HostableByMob)

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}
}
