package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.CompoundNBTTag
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolComponentType
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

class TestComponentType : SokolComponentType {
    override val key = KEY

    inner class Component(
        val info: Int
    ) : SokolComponent {
        override val type get() = this@TestComponentType

        override fun serialize(node: ConfigurationNode) {
            node.node("info").set(info)
        }

        override fun serialize(tag: CompoundNBTTag.Mutable) {
            tag.set("info") { ofInt(info) }
        }
    }

    override fun deserialize(node: ConfigurationNode) = Component(
        node.node("info").get { 0 },
    )

    override fun deserialize(tag: CompoundNBTTag) = Component(
        tag.int("info", 0)
    )

    companion object {
        val KEY = Key.key("sokol", "test_component")
    }
}
