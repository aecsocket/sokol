package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.bukkit.parsers.location.LocationArgument
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument
import com.gitlab.aecsocket.alexandria.core.extension.MSPT
import com.gitlab.aecsocket.alexandria.core.extension.TPS
import com.gitlab.aecsocket.alexandria.core.extension.value
import com.gitlab.aecsocket.alexandria.paper.BaseCommand
import com.gitlab.aecsocket.alexandria.paper.Context
import com.gitlab.aecsocket.alexandria.paper.desc
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.util.Timings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

private val STATS_INTERVALS = listOf(
    5 * TPS,
    30 * TPS,
    60 * TPS,
)

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
            .argument(StringArgument.of("id"), desc("TODO: id of bp"))
            .argument(MultiplePlayerSelectorArgument.optional("targets"), desc("Players to give to."))
            .argument(IntegerArgument.newBuilder<CommandSender?>("amount")
                .withMin(1)
                .asOptional(), desc("Amount of items to give."))
            .permission(perm("give"))
            .handler { handle(it, ::give) })
        manager.command(root
            .literal("summon", desc("Creates and summons an entity blueprint."))
            .argument(StringArgument.of("id"), desc("TODO: id of bp"))
            .argument(LocationArgument.of("location"), desc("Where to spawn the entity."))
            .permission(perm("summon"))
            .senderType(Player::class.java)
            .handler { handle(it, ::summon) })
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

        sendTimings(plugin.resolverTimings, "stats.resolver_timings")

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
        val blueprint = plugin.entityBlueprints[ctx.get("id")] ?: throw Exception() // todo
        val location = ctx.get<Location>("location")
        sender as Player

        blueprint.spawnEntity(location)
    }
}
