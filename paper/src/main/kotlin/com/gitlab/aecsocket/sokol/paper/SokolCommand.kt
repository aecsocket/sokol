package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.MSPT
import com.gitlab.aecsocket.alexandria.core.extension.TPS
import com.gitlab.aecsocket.alexandria.paper.BaseCommand
import com.gitlab.aecsocket.alexandria.paper.Context
import com.gitlab.aecsocket.alexandria.paper.desc
import com.gitlab.aecsocket.glossa.core.I18N
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.command.CommandSender

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
    }

    private fun stats(ctx: Context, sender: CommandSender, i18n: I18N<Component>) {
        plugin.sendMessage(sender, i18n.csafe("stats.header") {
            icu("time_last", plugin.objectResolver.resolveTimes.last())
        })

        STATS_INTERVALS.forEach { intervalTicks ->
            val intervalMs = (intervalTicks * MSPT).toDouble()
            val timings = plugin.objectResolver.resolveTimes.take(intervalTicks)
            plugin.sendMessage(sender, i18n.csafe("stats.timing") {
                icu("interval_ms", intervalMs)
                icu("interval_sec", intervalMs / 1000.0)
                icu("time_avg", timings.average())
                icu("time_min", timings.min())
                icu("time_max", timings.max())
            })
        }

        plugin.sendMessage(sender, i18n.csafe("stats.object_types"))

        plugin.objectResolver.lastStats.forEach { (type, stats) ->
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
}
