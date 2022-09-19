package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import org.spongepowered.configurate.ConfigurationNode

private const val ID = "id"
private const val HOSTABLE_BY_ITEM = "hostable_by_item"
private const val HOSTABLE_BY_ENTITY = "hostable_by_entity"

/*
class HostableByItem : PersistentComponent {
    @ConfigSerializable
    data class Config(
        @NodeKey override val id: String,
        @Setting(nodeFromParent = true) val descriptor: ItemDescriptor
    ) : Keyed

    private val _configs = Registry.create<Config>()
    val configs: Registry<Config> get() = _configs

    override val key get() = HostableByItem.key

    fun config(key: String) = _configs[key]
        ?: throw IllegalArgumentException("Invalid config ID '$key'")

    internal fun clearConfigs() = _configs.clear()

    internal fun load(log: LogList, node: ConfigurationNode) {
        node.node(HOSTABLE_BY_ITEM).childrenMap().forEach { (key, child) ->
            try {
                Keyed.validate(key.toString())
            } catch (ex: Keyed.ValidationException) {
                throw SerializationException(node, Config::class.java, "Invalid key", ex)
            }
            _configs.register(child.force())
        }
    }

    override fun deserialize(node: ConfigurationNode) = Component(
        config(node.node(ID).force())
    )

    override fun deserialize(tag: CompoundNBTTag) = Component(
        config(tag.string(ID))
    )

    inner class Component(
        val config: Config
    ) : SokolComponent.Persistent {
        override val key get() = HostableByItem.key

        override fun serialize(node: ConfigurationNode) {
            node.node(ID).set(config.id)
        }

        override fun serialize(tag: CompoundNBTTag.Mutable) {
            tag.set(ID) { ofString(config.id) }
        }
    }

    companion object : ComponentKey<Component> {
        override val key = SokolAPI.key(HOSTABLE_BY_ITEM)
    }
}*/

class HostableByEntity : PersistentComponent {
    override val type get() = HostableByEntity
    override val key get() = Key

    override fun serialize(tag: CompoundNBTTag.Mutable) {}

    override fun serialize(node: ConfigurationNode) {}

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun deserialize(tag: CompoundNBTTag) = HostableByEntity()

        override fun deserialize(node: ConfigurationNode) = HostableByEntity()
    }

    companion object : ComponentType<HostableByEntity> {
        val Key = SokolAPI.key(HOSTABLE_BY_ENTITY)
    }
}
