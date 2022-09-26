package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.bukkit.argument.NamespacedKeyArgument
import cloud.commandframework.bukkit.parsers.location.LocationArgument
import cloud.commandframework.bukkit.parsers.selector.MultipleEntitySelectorArgument
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument
import com.gitlab.aecsocket.alexandria.core.command.ConfigurationNodeArgument
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.BaseCommand
import com.gitlab.aecsocket.alexandria.paper.Context
import com.gitlab.aecsocket.alexandria.paper.desc
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.SokolBlueprint
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.util.Timings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

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
            .argument(StringArgument.of("id"), desc("TODO: id of bp"))
            .argument(MultiplePlayerSelectorArgument.optional("targets"), desc("Players to give to."))
            .argument(IntegerArgument.newBuilder<CommandSender?>("amount")
                .withMin(1)
                .asOptional(), desc("Amount of items to give."))
            .permission(perm("give"))
            .handler { handle(it, ::give) })
        manager.command(root
            .literal("summon", desc("Creates and summons an entity blueprint."))
            .argument(EntityBlueprintArgument(plugin, "blueprint"), desc("The blueprint to use."))
            .argument(LocationArgument.of("location"), desc("Where to spawn the entity."))
            .argument(IntegerArgument.newBuilder<CommandSender?>("amount")
                .withMin(1)
                .asOptional(), desc("The amount of entities to spawn."))
            .permission(perm("summon"))
            .handler { handle(it, ::summon) })

        val state = root
            .literal("state", desc(""))
        val stateRead = state
            .literal("read", desc(""))
        manager.command(stateRead
            .literal("mob", desc(""))
            .argument(MultipleEntitySelectorArgument.of("targets"), desc(""))
            .argument(NamespacedKeyArgument.builder<CommandSender>("component-type")
                .withSuggestionsProvider { _, _ -> plugin.componentTypes.keys.toList() }
                .asOptional(), desc(""))
            .permission(perm("state.read.entity"))
            .handler { handle(it, ::stateReadMob) })
        val stateWrite = state
            .literal("write", desc(""))
        manager.command(stateWrite
            .literal("mob", desc(""))
            .argument(MultipleEntitySelectorArgument.of("targets"), desc(""))
            .argument(ConfigurationNodeArgument("data", { AlexandriaAPI.configLoader().buildAndLoadString(it) }), desc(""))
            .permission(perm("state.write.entity"))
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
        /*val blueprint = plugin.itemBlueprints[ctx.get("id")] ?: throw Exception() // todo
        val targets = ctx.players("targets", sender, i18n)
        val amount = ctx.value("amount") { 1 }

        val item = blueprint.createItem()

        val stackSize = item.type.maxStackSize
        val stacksNum = amount / stackSize
        val remainderNum = amount % stackSize

        val fullStack = listOf(item.clone().apply { setAmount(stackSize) })
        val stacks = (
            (0 until stacksNum).flatMap { fullStack } +
            listOf(item.clone().apply { setAmount(remainderNum) })
        ).toTypedArray()

        targets.forEach { target ->
            target.inventory.addItem(*stacks)
        }*/
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

    fun stateReadMob(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val targets = ctx.entities("targets", sender, i18n)
        val componentType = ctx.getOptional<NamespacedKey>("component-type")
            .map { plugin.componentType(it) ?: error(i18n.safe("error.invalid_component_type") {
                subst("component_type", text(it.toString()))
            }) }
            .orElse(null)

        var results = 0
        targets.forEach { target ->
            plugin.persistence.getTag(target.persistentDataContainer, plugin.persistence.entityKey)?.let { tag ->
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
                    subst("entity", target.name())
                })

                plugin.sendMessage(sender, render.flatMap { line ->
                    i18n.csafe("state.read.line") {
                        subst("line", line)
                    }
                })

                results++
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
