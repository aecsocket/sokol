package com.github.aecsocket.sokol.core.stat

import com.github.aecsocket.alexandria.core.IntMod
import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.sokol.core.ItemDescriptor
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

class ItemDescriptorStat(namespace: String, key: String) : AbstractStat<ItemDescriptor>(namespace, key) {
    override fun deserialize(node: ConfigurationNode) = opDeserialize(node,
        // todo check the arg exists
        "set" to { Set(it[0].force()) },
        "modify" to { it[0].force<Modify>() }
    )

    data class Set(val value: ItemDescriptor) : Stat.Value.Discarding<ItemDescriptor> {
        override fun first() = value
    }

    @ConfigSerializable
    data class Modify(
        val modelData: IntMod,
        val damage: IntMod,
        val unbreakable: Boolean?
    ) : Stat.Value<ItemDescriptor> {
        override fun next(last: ItemDescriptor) = last.copy(
            modelData = modelData.apply(last.modelData),
            damage = damage.apply(last.damage),
            unbreakable = unbreakable ?: last.unbreakable
        )
    }
}
