package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.bukkit.argument.NamespacedKeyArgument
import cloud.commandframework.bukkit.parsers.location.LocationArgument
import cloud.commandframework.bukkit.parsers.selector.MultipleEntitySelectorArgument
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument
import com.gitlab.aecsocket.alexandria.core.command.ConfigurationNodeArgument
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.command.PlayerInventorySlotArgument
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.CompoundNBTTag
import com.gitlab.aecsocket.sokol.core.SokolBlueprint
import com.gitlab.aecsocket.sokol.core.util.Timings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.Component.translatable
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import java.util.Optional

private val STATS_INTERVALS = listOf(
    5 * TPS,
    30 * TPS,
    60 * TPS,
)

internal class SokolCommand(
    override val plugin: Sokol
) : BaseCommand(plugin) {
    init {
        captions?.registerMessageFactory(EntityBlueprintArgument.ARGUMENT_PARSE_FAILURE_ENTITY_BLUEPRINT, captionLocalizer)

        manager.command(root
            .literal("stats", desc("Show stats for the object resolver."))
            .permission(perm("stats"))
            .handler { handle(it, ::stats) })
        manager.command(root
            .literal("give", desc("Creates and gives an item blueprint to a player."))
            .argument(ItemBlueprintArgument(plugin, "blueprint"), desc("The blueprint to use."))
            .argument(MultiplePlayerSelectorArgument.optional("targets"), desc("Players to give to."))
            .argument(IntegerArgument.newBuilder<CommandSender>("amount")
                .withMin(1)
                .asOptional(), desc("Amount of items to give."))
            .permission(perm("give"))
            .handler { handle(it, ::give) })
        manager.command(root
            .literal("summon", desc("Creates and summons an entity blueprint."))
            .argument(EntityBlueprintArgument(plugin, "blueprint"), desc("The blueprint to use."))
            .argument(LocationArgument.of("location"), desc("Where to spawn the entity."))
            .argument(IntegerArgument.newBuilder<CommandSender>("amount")
                .withMin(1)
                .asOptional(), desc("The amount of entities to spawn."))
            .permission(perm("summon"))
            .handler { handle(it, ::summon) })

        fun <C> NamespacedKeyArgument.Builder<C>.componentType() =
            withSuggestionsProvider { _, _ -> plugin.componentTypes.keys.toList() }
            .asOptional()

        val state = root
            .literal("state", desc("Access the state of an entity in the world."))
        val stateRead = state
            .literal("read", desc("Reads the state of an entity in the world."))
        manager.command(stateRead
            .literal("mob", desc("Reads the state of the entity in a mob."))
            .argument(MultipleEntitySelectorArgument.of("targets"), desc("Mobs to read entities from."))
            .argument(NamespacedKeyArgument.builder<CommandSender>("component-type")
                .componentType(), desc("Key of the component to read."))
            .permission(perm("state.read.mob"))
            .handler { handle(it, ::stateReadMob) })
        manager.command(stateRead
            .literal("item", desc("Reads the state of an item in a player's inventory."))
            .argument(PlayerInventorySlotArgument("slot"), desc("Slot to read item from."))
            .argument(MultiplePlayerSelectorArgument.optional("targets"), desc("Players to get items from."))
            .argument(NamespacedKeyArgument.builder<CommandSender>("component-type")
                .componentType(), desc("Key of the component to read."))
            .permission(perm("state.read.item"))
            .handler { handle(it, ::stateReadItem) })
        val stateWrite = state
            .literal("write", desc(""))
        manager.command(stateWrite
            .literal("mob", desc(""))
            .argument(MultipleEntitySelectorArgument.of("targets"), desc(""))
            .argument(ConfigurationNodeArgument("data", { AlexandriaAPI.configLoader().buildAndLoadString(it) }), desc(""))
            .permission(perm("state.write.mob"))
            .handler { handle(it, ::stateWriteMob) })
    }

    fun stats(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        fun sendTimings(baseTimings: Timings, headerKey: String) {
            plugin.sendMessage(sender, i18n.csafe(headerKey) {
                icu("time_last", baseTimings.last())
            })

            STATS_INTERVALS.forEach { intervalTicks ->
                val intervalMs = (intervalTicks * MSPT).toDouble()
                val timings = baseTimings.takeLast(intervalTicks)
                plugin.sendMessage(sender, i18n.csafe("stats.timing") {
                    icu("interval_ms", intervalMs)
                    icu("interval_sec", intervalMs / 1000.0)
                    icu("time_avg", timings.average())
                    icu("time_min", timings.min())
                    icu("time_max", timings.max())
                })
            }
        }

        sendTimings(plugin.engineTimings, "stats.engine_timings")

        plugin.sendMessage(sender, i18n.csafe("stats.object_types"))

        plugin.entityResolver.lastStats.forEach { (type, stats) ->
            val (candidates, updated) = stats
            val percent = updated.toDouble() / candidates
            plugin.sendMessage(sender, i18n.csafe("stats.object_type") {
                subst("type", text(type))
                icu("candidates", candidates)
                icu("updated", updated)
                icu("percent", percent)
            })
        }
    }

    fun give(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val blueprint = ctx.get<KeyedItemBlueprint>("blueprint")
        val targets = ctx.players("targets", sender, i18n)
        val amount = ctx.value("amount") { 1 }

        val item = blueprint.createItem()
        item.amount = amount

        targets.forEach { target ->
            target.inventory.addItem(item)
        }

        plugin.sendMessage(sender, i18n.csafe("give") {
            subst("id", text(blueprint.id))
            subst("target", if (targets.size == 1) targets.first().name() else text(targets.size))
            icu("amount", amount)
        })
    }

    fun summon(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val blueprint = ctx.get<KeyedEntityBlueprint>("blueprint")
        val location = ctx.get<Location>("location")
        val amount = ctx.value("amount") { 1 }

        repeat(amount) {
            blueprint.spawnEntity(location)
        }

        plugin.sendMessage(sender, i18n.csafe("summon") {
            subst("id", text(blueprint.id))
            icu("amount", amount)
        })
    }

    fun stateReadComponentType(i18n: I18N<Component>, key: Optional<NamespacedKey>) = key
        .map { plugin.componentType(it) ?: error(i18n.safe("error.invalid_component_type") {
            subst("component_type", text(it.toString()))
        }) }
        .orElse(null)

    fun stateRead(
        ctx: Context,
        sender: CommandSender,
        i18n: I18N<Component>,
        componentType: PersistentComponentType?,
        tag: CompoundNBTTag,
        name: Component,
    ) {
        val configNode = AlexandriaAPI.configLoader().build().createNode()
        plugin.persistence.readBlueprint(tag).components.forEach { component ->
            if (
                component is PersistentComponent
                && (componentType == null || component.key == componentType.key)
            ) {
                component.writeKeyed(configNode)
            }
        }

        val render = configNode.render()

        plugin.sendMessage(sender, i18n.csafe("state.read.header_${if (render.isEmpty()) "empty" else "present"}") {
            subst("entity", name)
        })

        plugin.sendMessage(sender, render.flatMap { line ->
            i18n.csafe("state.read.line") {
                subst("line", line)
            }
        })
    }

    fun stateReadMob(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val targets = ctx.entities("targets", sender, i18n)
        val componentType = stateReadComponentType(i18n, ctx.getOptional("component-type"))

        var results = 0
        targets.forEach { target ->
            plugin.persistence.getTag(target.persistentDataContainer, plugin.persistence.entityKey)?.let { tag ->
                stateRead(ctx, sender, i18n, componentType, tag, target.name())
                results++
            }
        }

        plugin.sendMessage(sender, i18n.csafe("state.read.complete") {
            icu("results", results)
        })
    }

    fun stateReadItem(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val slot = ctx.get<PlayerInventorySlot>("slot")
        val targets = ctx.players("targets", sender, i18n)
        val componentType = stateReadComponentType(i18n, ctx.getOptional("component-type"))

        var results = 0
        targets.forEach { target ->
            val item = slot.getFrom(target.inventory)
            if (item.hasItemMeta()) {
                val meta = item.itemMeta
                plugin.persistence.getTag(meta.persistentDataContainer, plugin.persistence.entityKey)?.let { tag ->
                    stateRead(ctx, sender, i18n, componentType, tag, meta.displayName() ?: translatable(item.translationKey()))
                    results++
                }
            }
        }
        plugin.sendMessage(sender, i18n.csafe("state.read.complete") {
            icu("results", results)
        })
    }

    fun stateWriteMob(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val targets = ctx.entities("targets", sender, i18n)
        val data = ctx.get<ConfigurationNode>("data")

        val blueprint = try {
            data.force<SokolBlueprint>()
        } catch (ex: SerializationException) {
            error(i18n.safe("error.parse_blueprint"), ex)
        }


        targets.forEach { target ->
            // TODO
        }
    }
}
