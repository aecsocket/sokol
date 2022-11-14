package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.bukkit.parsers.location.LocationArgument
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument
import com.gitlab.aecsocket.alexandria.core.extension.value
import com.gitlab.aecsocket.alexandria.paper.BaseCommand
import com.gitlab.aecsocket.alexandria.paper.Context
import com.gitlab.aecsocket.alexandria.paper.desc
import com.gitlab.aecsocket.craftbullet.core.Timings
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.ItemHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

private val TIMING_INTERVALS = listOf(5, 30, 60)

internal class SokolCommand(
    override val plugin: Sokol
) : BaseCommand(plugin) {
    init {
        manager.command(root
            .literal("stats", desc("Shows info on how the plugin is running."))
            .permission(perm("stats"))
            .handler { handle(it, ::stats) })
        manager.command(root
            .literal("systems", desc("Shows registered system information."))
            .permission(perm("systems"))
            .handler { handle(it, ::systems) })

        manager.command(root
            .literal("give", desc("Gives an entity item to a player."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), desc("Players to give to."))
            .argument(IntegerArgument.newBuilder<CommandSender>("amount")
                .withMin(1), desc("Amount of items to give."))
            .argument(EntityArgument(plugin, "entity"), desc("Entity to give."))
            .permission(perm("give"))
            .handler { handle(it, ::give) })
        manager.command(root
            .literal("summon", desc("Summons an entity mob."))
            .argument(LocationArgument.of("location"), desc("Where to spawn the mob."))
            .argument(IntegerArgument.newBuilder<CommandSender>("amount")
                .withMin(1), desc("Amount of mobs to spawn."))
            .argument(EntityArgument(plugin, "entity"), desc("Entity to spawn."))
            .permission(perm("summon"))
            .handler { handle(it, ::summon) })
    }

    private lateinit var mProfiled: ComponentMapper<Profiled>
    private lateinit var mItemHolder: ComponentMapper<ItemHolder>

    internal fun enable() {
        mProfiled = plugin.engine.mapper()
        mItemHolder = plugin.engine.mapper()
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

        sendTimings(plugin.timings, "stats.timings")

        plugin.sendMessage(sender, i18n.csafe("stats.object_types"))

        plugin.resolver.lastStats.forEach { (type, stats) ->
            val (candidates, updated) = stats
            val percent = updated.toDouble() / candidates
            plugin.sendMessage(sender, i18n.csafe("stats.object_type") {
                icu("type", type.key)
                icu("candidates", candidates)
                icu("updated", updated)
                icu("percent", percent)
            })
        }
    }

    fun systems(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val systemTypes = plugin.engine.systems()
            .mapNotNull { it::class.simpleName }

        plugin.sendMessage(sender, i18n.csafe("systems.header"))
        systemTypes.forEachIndexed { idx, type ->
            plugin.sendMessage(sender, i18n.csafe("systems.line") {
                icu("index", idx + 1)
                icu("type", type)
            })
        }
    }

    fun give(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val targets = ctx.players("targets", sender, i18n)
        val amount = ctx.value("amount") { 1 }
        val entity = ctx.get<SokolEntity>("entity")

        targets.forEach { target ->
            val targetEntity = entity.copyOf()
            mItemHolder.set(targetEntity, ItemHolder.byMob(target))
            val item = plugin.hoster.hostItem(targetEntity)
            item.amount = amount
            target.inventory.addItem(item)
        }

        if (sender is Player)
            mItemHolder.set(entity, ItemHolder.byMob(sender))
        val item = plugin.hoster.hostItem(entity)

        plugin.sendMessage(sender, i18n.csafe("give") {
            subst("item", item.displayName())
            subst("target", if (targets.size == 1) targets.first().name() else text(targets.size))
            icu("amount", amount)
        })
    }

    fun summon(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val location = ctx.get<Location>("location")
        val amount = ctx.value("amount") { 1 }
        val entity = ctx.get<SokolEntity>("entity")

        repeat(amount) {
            plugin.hoster.hostMob(entity.copyOf(), location)
        }

        val profile = mProfiled.get(entity).profile

        plugin.sendMessage(sender, i18n.csafe("summon") {
            subst("id", text(profile.id))
            icu("amount", amount)
        })
    }

    /*init {
        manager.command(root
            .literal("stats", desc("Show stats for the object resolver."))
            .permission(perm("stats"))
            .handler { handle(it, ::stats) })
        manager.command(root
            .literal("systems", desc("See the system information."))
            .permission(perm("systems"))
            .handler { handle(it, ::systems) })
        manager.command(root
            .literal("give", desc("Creates and gives an item blueprint to a player."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), desc("Players to give to."))
            .argument(IntegerArgument.newBuilder<CommandSender>("amount")
                .withMin(1), desc("Amount of items to give."))
            .argument(KeyedEntityBlueprintArgument(plugin, "blueprint"), desc("The blueprint to use."))
            .permission(perm("give"))
            .handler { handle(it, ::give) })
        manager.command(root
            .literal("summon", desc("Creates and summons a mob blueprint."))
            .argument(LocationArgument.of("location"), desc("Where to spawn the mob."))
            .argument(IntegerArgument.newBuilder<CommandSender>("amount")
                .withMin(1), desc("The amount of mobs to spawn."))
            .argument(KeyedEntityBlueprintArgument(plugin, "blueprint"), desc("The blueprint to use."))
            .permission(perm("summon"))
            .handler { handle(it, ::summon) })

        val holding = root
            .literal("holding", desc("Tools for manipulating the current held item."))
        manager.command(holding
            .literal("freeze", desc("Freeze the held item in place."))
            .argument(BooleanArgument.optional("enable"), desc("If the item should be frozen."))
            .permission(perm("holding.freeze"))
            .senderType(Player::class.java)
            .handler { handle(it, ::holdingFreeze) })
        manager.command(holding
            .literal("shape", desc("Draw the shape from a config on the held item."))
            .argument(ConfigurationNodeArgument("config", { AlexandriaAPI.configLoader().buildAndLoadString(it) }), desc("The configuration node."))
            .permission(perm("holding.shape"))
            .senderType(Player::class.java)
            .handler { handle(it, ::holdingShape) })
        manager.command(holding
            .literal("slot-shapes", desc("Draw the shapes of all slots on the held item."))
            .argument(BooleanArgument.optional("enable"), desc("If the slots should be drawn."))
            .permission(perm("holding.slot-shapes"))
            .senderType(Player::class.java)
            .handler { handle(it, ::holdingSlotShapes) })

        val state = root
            .literal("state", desc("Access the state of an entity in the world."))
        val stateRead = state
            .literal("read", desc("Read the state of an entity in the world."))
        manager.command(stateRead
            .literal("mob", desc("Read from a mob."))
            .argument(MultipleEntitySelectorArgument.of("targets"), desc("Mobs to read entities from."))
            .permission(perm("state.read.mob"))
            .handler { handle(it, ::stateReadMob) })
        manager.command(stateRead
            .literal("item", desc("Read from an item in a player inventory."))
            .argument(PlayerInventorySlotArgument("slot", required = false), desc("Slot to read item from."))
            .argument(MultiplePlayerSelectorArgument.optional("targets"), desc("Players to get items from."))
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

        val composite = root
            .literal("composite", desc("Read the info of a composite tree of an entity."))
        manager.command(composite
            .literal("mob", desc("Read from a mob."))
            .argument(MultipleEntitySelectorArgument.of("targets"), desc("Mobs to read entities from."))
            .permission(perm("composite.mob"))
            .handler { handle(it, ::compositeReadMob) })
        manager.command(composite
            .literal("item", desc("Read from an item in a player inventory."))
            .argument(PlayerInventorySlotArgument("slot", required = false), desc("Slot to read item from."))
            .argument(MultiplePlayerSelectorArgument.optional("targets"), desc("Players to get items from."))
            .permission(perm("composite.mob"))
            .handler { handle(it, ::compositeReadItem) })
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

        plugin.resolver.lastStats.forEach { (type, stats) ->
            val (candidates, updated) = stats
            val percent = updated.toDouble() / candidates
            plugin.sendMessage(sender, i18n.csafe("stats.object_type") {
                icu("type", type.key)
                icu("candidates", candidates)
                icu("updated", updated)
                icu("percent", percent)
            })
        }
    }

    fun systems(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val systemTypes = plugin.engine.systems()
            .mapNotNull { it::class.simpleName }

        plugin.sendMessage(sender, i18n.csafe("systems.header"))
        systemTypes.forEachIndexed { idx, type ->
            plugin.sendMessage(sender, i18n.csafe("systems.line") {
                icu("index", idx + 1)
                icu("type", type)
            })
        }
    }

    fun give(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val targets = ctx.players("targets", sender, i18n)
        val amount = ctx.value("amount") { 1 }
        val blueprint = ctx.get<EntityBlueprint>("blueprint")

        if (!plugin.hoster.canHost(blueprint, SokolObjectType.Item))
            error(i18n.safe("error.cannot_host"))

        targets.forEach { target ->
            val newBlueprint = blueprint.copyOf()
            newBlueprint.components.set(ItemHolder.byMob(target))
            val item = plugin.hoster.hostItem(newBlueprint)
            item.amount = amount
            target.inventory.addItem(item)
        }

        if (sender is Player)
            blueprint.components.set(ItemHolder.byMob(sender))
        val item = plugin.hoster.hostItem(blueprint)

        plugin.sendMessage(sender, i18n.csafe("give") {
            subst("item", item.displayName())
            subst("target", if (targets.size == 1) targets.first().name() else text(targets.size))
            icu("amount", amount)
        })
    }

    fun summon(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val location = ctx.get<Location>("location")
        val amount = ctx.value("amount") { 1 }
        val blueprint = ctx.get<KeyedEntityBlueprint>("blueprint")

        if (!plugin.hoster.canHost(blueprint, SokolObjectType.Mob))
            error(i18n.safe("error.cannot_host"))

        repeat(amount) {
            plugin.hoster.hostMob(blueprint, location)
        }

        plugin.sendMessage(sender, i18n.csafe("summon") {
            subst("id", text(blueprint.profile.id))
            icu("amount", amount)
        })
    }

    fun holdingFreeze(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val player = sender as Player
        val holding = player.alexandria.heldEntity ?: error(i18n.safe("error.not_holding"))
        val enable = ctx.value("enable") { !holding.frozen }

        holding.frozen = enable
        plugin.sendMessage(sender, i18n.csafe("holding.freeze.${if (enable) "enable" else "disable"}"))
    }

    fun holdingShape(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val player = sender as Player
        val holding = player.alexandria.heldEntity ?: error(i18n.safe("error.not_holding"))
        val shape = try {
            var node = ctx.get<ConfigurationNode>("config")
            // hocon serializers only accepts map nodes as root
            // if someone wants to define a compound `[...]` they use `{_:[...]}`
            if (node.hasChild("_"))
                node = node.node("_")
            node.force<Shape>()
        } catch (ex: SerializationException) {
            error(i18n.safe("error.parse_shape"), ex)
        }

        CraftBulletAPI.executePhysics {
            holding.drawShape = collisionOf(shape)
        }
    }

    fun holdingSlotShapes(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val player = sender as Player
        val holding = player.alexandria.heldEntity ?: error(i18n.safe("error.not_holding"))
        val enable = ctx.value("enable") { !holding.drawSlotShapes }

        holding.drawSlotShapes = enable
        plugin.sendMessage(sender, i18n.csafe("holding.slot_shapes.${if (enable) "enable" else "disable"}"))
    }

    fun stateRead(
        ctx: Context,
        sender: CommandSender,
        i18n: I18N<Component>,
        entity: SokolEntity,
        name: Component,
    ) {
        val components = entity.components.all()

        val componentTypes = components
            .mapNotNull { it.componentType.simpleName }
            .sorted()

        val hover = (
            i18n.csafe("state.read.component.header") +
            componentTypes.flatMap { type ->
                i18n.csafe("state.read.component.line") {
                    icu("type", type)
                }
            }
        ).join(JoinConfiguration.newlines())

        val node = AlexandriaAPI.configLoader().build().createNode()
        node.set(entity)

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

        var results = 0
        targets.forEach { target ->
            plugin.useMob(target, false) { entity ->
                stateRead(ctx, sender, i18n, entity, target.name())
                results++
            }
        }

        plugin.sendMessage(sender, i18n.csafe("state.read.complete") {
            icu("results", results)
        })
    }

    fun stateReadItem(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        val slot = ctx.value<PlayerInventorySlot>("slot") { PlayerInventorySlot.ByEquipment(EquipmentSlot.HAND) }
        val targets = ctx.players("targets", sender, i18n)

        var results = 0
        targets.forEach { target ->
            val item = slot.getFrom(target.inventory)
            plugin.useItem(item, false,
                { blueprint ->
                    blueprint.components.set(ItemHolder.byPlayer(target, slot.asInt(target.inventory)))
                }
            ) { entity ->
                stateRead(ctx, sender, i18n, entity, item.displayName())
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

    fun compositeReadMob(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        // todo
    }

    fun compositeReadItem(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        // todo
    }*/
}
