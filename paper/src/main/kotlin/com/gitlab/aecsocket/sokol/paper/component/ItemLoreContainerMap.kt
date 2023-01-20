package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.TableAligns
import com.gitlab.aecsocket.alexandria.core.TableCell
import com.gitlab.aecsocket.alexandria.core.TableRow
import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.REPLACE_MARKER
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class ItemLoreContainerMap(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_container_map")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { ItemLoreContainerMapSystem(it) }
        }
    }

    override val componentType get() = ItemLoreContainerMap::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val baseKey: String,
        @Required val rows: List<String>,
        val tableAligns: TableAligns = TableAligns.Default
    ) : SimpleComponentProfile<ItemLoreContainerMap> {
        override val componentType get() = ItemLoreContainerMap::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreContainerMap(this) }
    }
}

private const val COLUMN_SEPARATOR = "column_separator"
private const val SLOT_EMPTY = "slot_empty"
private const val SLOT_UNKNOWN = "slot_unknown"
private const val SLOT = "slot"

@All(ItemLoreContainerMap::class, ItemLoreManager::class, ContainerMap::class)
class ItemLoreContainerMapSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Lore = ItemLoreContainerMap.Key.with("lore")
    }

    private val mItemLoreContainerMap = ids.mapper<ItemLoreContainerMap>()
    private val mItemLoreManager = ids.mapper<ItemLoreManager>()
    private val mContainerMap = ids.mapper<ContainerMap>()
    private val mEntitySlotInMap = ids.mapper<EntitySlotInMap>()
    private val mDisplayName = ids.mapper<DisplayName>()

    private fun ItemLoreContainerMap.Profile.key(path: String) = "$baseKey.$path"

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val itemLoreContainerMap = mItemLoreContainerMap.get(entity).profile
        val itemLoreManager = mItemLoreManager.get(entity)

        itemLoreManager.loreProvider(Lore) { i18n ->
            val containerMap = mContainerMap.get(entity)

            val rows: List<TableRow<Component>> = itemLoreContainerMap.rows.mapNotNull { key ->
                val slotEntity = containerMap.child(key) ?: return@mapNotNull null
                // if the child isn't a slot entity, we don't include it
                // (this child was probably added retroactively)
                val slot = mEntitySlotInMap.getOr(slotEntity) ?: return@mapNotNull null
                val slotContainer = mContainerMap.getOr(slotEntity) ?: return@mapNotNull null
                val childEntity = slot.childOf(slotContainer)

                val keyCell = i18n.safe(itemLoreContainerMap.key("$SLOT.$key"))
                val childCell = childEntity
                    ?.let {
                        mDisplayName.getOr(childEntity)?.nameFor(i18n)?.let { listOf(it) }
                            ?: i18n.safe(itemLoreContainerMap.key(SLOT_UNKNOWN))
                    } ?: i18n.safe(itemLoreContainerMap.key(SLOT_EMPTY))
                listOf(keyCell, childCell)
            }

            val columnSeparator = i18n.makeOne(itemLoreContainerMap.key(COLUMN_SEPARATOR)) ?: Component.empty()
            AlexandriaAPI.ComponentTableRenderer(
                align = itemLoreContainerMap.tableAligns.aligner(),
                justify = itemLoreContainerMap.tableAligns.justifier(),
                colSeparator = columnSeparator,
                rowSeparator = { emptyList() }
            ).render(rows)
        }
    }
}
