package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import org.spongepowered.configurate.ConfigurationNode

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
    }
}
