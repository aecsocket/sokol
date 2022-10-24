package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.NodeKey
import org.spongepowered.configurate.objectmapping.meta.Setting

private const val HOSTABLE_BY_ITEM = "hostable_by_item"

data class HostableByItem(
    val backing: Config
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("hostable_by_item")
    }

    override val componentType get() = HostableByItem::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeString(backing.id)

    override fun write(node: ConfigurationNode) {
        node.set(backing.id)
    }

    @ConfigSerializable
    data class Config(
        @NodeKey override val id: String,
        @Setting(nodeFromParent = true) val descriptor: ItemDescriptor,
    ) : Keyed

    class Type : RegistryComponentType<Config>(Config::class, HOSTABLE_BY_ITEM) {
        override val key get() = Key

        override fun read(tag: NBTTag) = HostableByItem(
            entry(tag.asString())
        )

        override fun read(node: ConfigurationNode) = HostableByItem(
            entry(node.force())
        )

        override fun readFactory(node: ConfigurationNode): PersistentComponentFactory {
            val backing = entry(node.force())
            return PersistentComponentFactory { HostableByItem(backing) }
        }
    }
}

class HostableByEntity : PersistentComponent {

    companion object {
        val Key = SokolAPI.key("hostable_by_entity")
    }

    override val componentType get() = HostableByEntity::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = HostableByEntity()

        override fun read(node: ConfigurationNode) = HostableByEntity()

        override fun readFactory(node: ConfigurationNode) = PersistentComponentFactory { HostableByEntity() }
    }
}
