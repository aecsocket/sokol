package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.ItemDescriptor
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolHost
import org.bukkit.inventory.ItemStack

sealed interface Blueprint : Keyed

class ItemBlueprint(
    private val sokol: Sokol,
    override val id: String,
    val item: ItemDescriptor,
    val components: List<SokolComponent>,
) : Blueprint {
    fun create(host: SokolHost): ItemStack {
        val entity = PaperEntity(host)
        components.forEach { entity.addComponent(it) }
        return item.create().withMeta { meta ->
            val tag = sokol.persistence.newTag()
            sokol.persistence.writeEntity(entity, tag)
            sokol.persistence.writeTagTo(tag, sokol.persistence.entityKey, meta.persistentDataContainer)
        }
    }
}
