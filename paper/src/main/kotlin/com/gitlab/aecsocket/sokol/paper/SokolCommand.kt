package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument
import com.gitlab.aecsocket.alexandria.core.extension.MSPT
import com.gitlab.aecsocket.alexandria.core.extension.TPS
import com.gitlab.aecsocket.alexandria.core.extension.value
import com.gitlab.aecsocket.alexandria.paper.BaseCommand
import com.gitlab.aecsocket.alexandria.paper.Context
import com.gitlab.aecsocket.alexandria.paper.desc
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.SokolHost
import com.gitlab.aecsocket.sokol.core.Timings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

private const val ITPS = TPS.toInt()
private val STATS_INTERVALS = listOf(
    5 * ITPS,
    30 * ITPS,
    60 * ITPS,
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
            .literal("give", desc("Creates and gives an item stack to a player."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), desc("Players to give to."))
            .argument(StringArgument.of("id"), desc("TODO: id of bp"))
            .argument(IntegerArgument.newBuilder<CommandSender?>("amount")
                .withMin(1)
                .asOptional(), desc("Amount of items to give."))
            .permission(perm("give"))
            .handler { handle(it, ::give) })
    }

    fun stats(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        fun sendTimings(baseTimings: Timings, headerKey: String) {
            plugin.sendMessage(sender, i18n.csafe(headerKey) {
                icu("time_last", baseTimings.last())
            })

            STATS_INTERVALS.forEach { intervalTicks ->
                val intervalMs = (intervalTicks * MSPT).toDouble()
                val timings = baseTimings.take(intervalTicks)
                plugin.sendMessage(sender, i18n.csafe("stats.timing") {
                    icu("interval_ms", intervalMs)
                    icu("interval_sec", intervalMs / 1000.0)
                    icu("time_avg", timings.average())
                    icu("time_min", timings.min())
                    icu("time_max", timings.max())
                })
            }
        }

        sendTimings(plugin.entityResolver.timings, "stats.resolver_timings")

        sendTimings(plugin.engine.timings, "stats.engine_timings")

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
        val targets = ctx.players("targets", sender, i18n)
        val blueprint = plugin.itemBlueprints[ctx.get("id")] ?: throw Exception() // todo
        val amount = ctx.value("amount") { 1 }

        val item = blueprint.create(object : SokolHost {})

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
        }
    }
}
