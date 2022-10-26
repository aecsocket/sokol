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
import com.gitlab.aecsocket.craftbullet.core.Timings
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.ItemHolder
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.spongepowered.configurate.ConfigurationNode
import java.util.Optional

private val TIMING_INTERVALS = listOf(5, 30, 60)

internal class SokolCommand(
    override val plugin: Sokol
) : BaseCommand(plugin) {
    init {
        manager.command(root
            .literal("stats", desc("Show stats for the object resolver."))
            .permission(perm("stats"))
            .handler { handle(it, ::stats) })
        manager.command(root
            .literal("give", desc("Creates and gives an item blueprint to a player."))
            .argument(EntityBlueprintArgument(plugin, "blueprint"), desc("The blueprint to use."))
            .argument(MultiplePlayerSelectorArgument.optional("targets"), desc("Players to give to."))
            .argument(IntegerArgument.newBuilder<CommandSender>("amount")
                .withMin(1)
                .asOptional(), desc("Amount of items to give."))
            .permission(perm("give"))
            .handler { handle(it, ::give) })
        manager.command(root
            .literal("summon", desc("Creates and summons a mob blueprint."))
            .argument(EntityBlueprintArgument(plugin, "blueprint"), desc("The blueprint to use."))
            .argument(LocationArgument.of("location"), desc("Where to spawn the mob."))
            .argument(IntegerArgument.newBuilder<CommandSender>("amount")
                .withMin(1)
                .asOptional(), desc("The amount of mobs to spawn."))
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
            .argument(PlayerInventorySlotArgument("slot", required = false), desc("Slot to read item from."))
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
                icu("time_last", baseTimings.allEntries().lastOrNull() ?: "?")
            })

            TIMING_INTERVALS.forEach { interval ->
                val intervalMs = interval * 1000L
                val timings = baseTimings.lastEntries(intervalMs)
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
                subst("type", text(type.key))
                icu("candidates", candidates)
                icu("updated", updated)
                icu("percent", percent)
            })
        }
    }

    fun give(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val blueprint = ctx.get<EntityBlueprint>("blueprint")
        val targets = ctx.players("targets", sender, i18n)
        val amount = ctx.value("amount") { 1 }

        if (!plugin.entityHoster.canHost(blueprint, SokolObjectType.Item))
            error(i18n.safe("error.cannot_host"))

        targets.forEach { target ->
            val newBlueprint = blueprint.copyOf()
            newBlueprint.components.set(ItemHolder.byMob(target))
            val item = plugin.entityHoster.hostItem(newBlueprint)
            item.amount = amount
            target.inventory.addItem(item)
        }

        if (sender is Player)
            blueprint.components.set(ItemHolder.byMob(sender))
        val item = plugin.entityHoster.hostItem(blueprint)

        plugin.sendMessage(sender, i18n.csafe("give") {
            subst("item", item.displayName())
            subst("target", if (targets.size == 1) targets.first().name() else text(targets.size))
            icu("amount", amount)
        })
    }

    fun summon(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val blueprint = ctx.get<EntityBlueprint>("blueprint")
        val location = ctx.get<Location>("location")
        val amount = ctx.value("amount") { 1 }

        if (!plugin.entityHoster.canHost(blueprint, SokolObjectType.Mob))
            error(i18n.safe("error.cannot_host"))

        repeat(amount) {
            plugin.entityHoster.hostMob(blueprint, location)
        }

        plugin.sendMessage(sender, i18n.csafe("summon") {
            subst("id", text(blueprint.profile.id))
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
        componentType: ComponentType?,
        entity: SokolEntity,
        name: Component,
    ) {
        val components = entity.components.all()

        val hover = (
            i18n.csafe("state.read.component.header") +
            components.flatMap { component ->
                i18n.csafe("state.read.component.line") {
                    icu("type", component::class.simpleName ?: "?")
                    icu("to_string", component.toString())
                }
            }
        ).join(JoinConfiguration.newlines())

        val node = AlexandriaAPI.configLoader().build().createNode()
        components.forEach { component ->
            if (
                component is PersistentComponent
                && (componentType == null || component.key == componentType.key)
            ) {
                component.write(node.node(component.key.asString()))
            }
        }

        val render = node.render()
        plugin.sendMessage(sender, i18n.csafe("state.read.header.${if (render.isEmpty()) "empty" else "present"}") {
            subst("entity", name)
        }.map { it.hoverEvent(hover) })

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
            plugin.useMob(target, false) { entity ->
                stateRead(ctx, sender, i18n, componentType, entity, target.name())
                results++
            }
        }

        plugin.sendMessage(sender, i18n.csafe("state.read.complete") {
            icu("results", results)
        })
    }

    fun stateReadItem(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val slot = ctx.getOptional<PlayerInventorySlot>("slot")
            .orElse(PlayerInventorySlot.ByEquipment(EquipmentSlot.HAND))
        val targets = ctx.players("targets", sender, i18n)
        val componentType = stateReadComponentType(i18n, ctx.getOptional("component-type"))

        var results = 0
        targets.forEach { target ->
            val item = slot.getFrom(target.inventory)
            plugin.useItem(item, false,
                { blueprint ->
                    blueprint.components.set(ItemHolder.byPlayer(target, slot.asInt(target.inventory)))
                }
            ) { entity ->
                stateRead(ctx, sender, i18n, componentType, entity, item.displayName())
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

        /*
        val dataMap = try {
            data.forceMap(SokolBlueprint::class.java).map { (_, child) ->
                val type = componentTypeFrom(plugin, SokolBlueprint::class.java, child)
                type to child
            }.associate { it }
        } catch (ex: SerializationException) {
            error(i18n.safe("error.parse_blueprint"), ex)
        }

        var results = 0
        targets.forEach { target ->
            plugin.useMob(target) { entity ->
                dataMap.forEach { (type, child) ->
                    val engineType = plugin.engine.componentType(type.componentType)
                    entity.getComponent(engineType)?.let { existing ->
                        if (existing is PersistentComponent) {
                            // merge cfg values
                            val configNode = AlexandriaAPI.configLoader().build().createNode()
                            existing.write(configNode)
                            child.mergeFrom(configNode)

                            val newComponent = try {
                                type.read(child)
                            } catch (ex: SerializationException) {
                                error(i18n.safe("error.parse_blueprint"), ex)
                            }

                            entity.setComponent(newComponent)
                        }
                    } ?: run {
                        // add new component
                        val newComponent = try {
                            type.read(child)
                        } catch (ex: SerializationException) {
                            error(i18n.safe("error.parse_blueprint"), ex)
                        }
                        entity.setComponent(newComponent)
                    }
                }
                results++
            }
        }

        plugin.sendMessage(sender, i18n.csafe("state.write.complete") {
            icu("results", results)
        })*/
    }
}
