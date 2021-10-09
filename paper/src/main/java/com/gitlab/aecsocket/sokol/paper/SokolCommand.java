package com.gitlab.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.context.CommandContext;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.paper.command.ComponentArgument;
import com.gitlab.aecsocket.sokol.paper.impl.PaperComponent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;

public final class SokolCommand extends BaseCommand<SokolPlugin> {
    private static final NamedTextColor separator = NamedTextColor.GRAY;

    private final Localizer lc;

    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));
        lc = plugin.lc();

        manager.command(root
                .literal("component", ArgumentDescription.of("Outputs detailed information on a registered component."))
                .argument(ComponentArgument.of(plugin, "component"))
                .permission("%s.command.component".formatted(rootName))
                .handler(c -> handle(c, this::give)));
    }

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperComponent component = ctx.get("component");

        send(sender, locale, "component.header",
                "type", component.getClass().getSimpleName(),
                "name", component.render(locale, lc),
                "id", component.id());
        send(sender, locale, "component.tags",
                "tags", String.join(", ", component.tags()));

        send(sender, locale, "component.slots",
                "amount", component.slots().size()+"");
        for (var entry : component.slots().entrySet()) {
            String key = entry.getKey();
            PaperSlot slot = entry.getValue();

            lc(locale, PREFIX_COMMAND + ".component.slot",
                    "name", slot.render(locale, lc),
                    "key", key,
                    "tags", String.join(", ", slot.tags()),
                    "offset", slot.offset()+"")
                    .ifPresent(msg -> sender.sendMessage(msg.hoverEvent(slot.rule().render(locale, lc))));
        }

        send(sender, locale, "component.features",
                "amount", component.features().size()+"");
        for (var entry : component.features().entrySet()) {
            String id = entry.getKey();
            PaperFeature<?> feature = entry.getValue();

            lc(locale, PREFIX_COMMAND + ".component.feature",
                    "name", feature.render(locale, lc),
                    "id", id,
                    "description", feature.renderDescription(locale, lc))
                    .ifPresent(msg -> sender.sendMessage(msg.hoverEvent(join(separator(newline()), feature.renderConfig(locale, lc)))));
        }

        List<StatIntermediate.MapData> stats = component.stats().join();
        send(sender, locale, "component.stats",
                "amount", stats.size()+"");
        for (var data : stats) {
            StatMap map = data.stats();

            List<Component> lines = new ArrayList<>();
            for (var entry : map.entrySet()) {
                String key = entry.getKey();
                Stat.Node<?> node = entry.getValue();
                List<? extends Stat.Node<?>> chain = node.asList();
                lc(locale, PREFIX_COMMAND + ".component.stat",
                        "name", node.stat().render(locale, lc),
                        "key", key,
                        "nodes", chain.size()+"",
                        "chain", join(separator(text(", ", separator)),
                                chain.stream().map(n -> n.value().render(locale, lc)).collect(Collectors.toList())))
                        .ifPresent(lines::add);
            }

            Component hover = join(separator(newline()), lines);
            lc(locale, PREFIX_COMMAND + ".component.stat_map",
                    "priority", data.priority().toString(),
                    "amount", map.size()+"",
                    "rule", data.rule().render(locale, lc))
                    .ifPresent(msg -> sender.sendMessage(msg.hoverEvent(hover)));
        }
    }
}
