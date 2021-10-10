package com.gitlab.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.context.CommandContext;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.paper.command.BlueprintArgument;
import com.gitlab.aecsocket.sokol.paper.command.ComponentArgument;
import com.gitlab.aecsocket.sokol.paper.impl.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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

    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        manager.command(root
                .literal("component", ArgumentDescription.of("Outputs detailed information on a registered component."))
                .argument(ComponentArgument.of(plugin, "component"))
                .permission("%s.command.component".formatted(rootName))
                .handler(c -> handle(c, this::component)));
        manager.command(root
                .literal("blueprint", ArgumentDescription.of("Outputs detailed information on a registered blueprint."))
                .argument(BlueprintArgument.of(plugin, "blueprint"))
                .permission("%s.command.blueprint".formatted(rootName))
                .handler(c -> handle(c, this::blueprint)));
    }

    private void component(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperComponent component = ctx.get("component");

        send(sender, locale, "component.header",
                "type", component.getClass().getSimpleName(),
                "name", component.render(locale, lc),
                "id", component.id());

        component.renderDescription(locale, lc).ifPresent(desc -> {
            for (var line : desc)
                send(sender, locale, "component.description",
                        "line", line);
        });
        send(sender, locale, "component.tags",
                "tags", String.join(", ", component.tags()));

        send(sender, locale, "component.slots",
                "amount", component.slots().size()+"");
        for (var entry : component.slots().entrySet()) {
            String key = entry.getKey();
            PaperSlot slot = entry.getValue();

            Component hover = slot.rule().render(locale, lc);
            lc.lines(locale, PREFIX_COMMAND + ".component.slot",
                    "name", slot.render(locale, lc),
                    "key", key,
                    "tags", String.join(", ", slot.tags()),
                    "offset", slot.offset()+"")
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
        }

        send(sender, locale, "component.features",
                "amount", component.features().size()+"");
        for (var entry : component.features().entrySet()) {
            String id = entry.getKey();
            PaperFeature<?> feature = entry.getValue();

            Component hover = feature.renderConfig(locale, lc)
                    .map(cfg -> join(separator(newline()), cfg))
                    .orElse(empty());
            lc.lines(locale, PREFIX_COMMAND + ".component.feature",
                    "name", feature.render(locale, lc),
                    "id", id,
                    "description", feature.renderDescription(locale, lc))
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
        }

        List<StatIntermediate.MapData> stats = component.stats().join();
        send(sender, locale, "component.stats",
                "amount", stats.size()+"");
        for (var data : stats) {
            StatMap map = data.stats();

            List<Component> lines = new ArrayList<>();
            lc.lines(locale, PREFIX_COMMAND + ".component.stat_header",
                    "amount", map.size()+"")
                    .ifPresent(lines::addAll);
            for (var entry : map.entrySet()) {
                String key = entry.getKey();
                Stat.Node<?> node = entry.getValue();
                List<? extends Stat.Node<?>> chain = node.asList();
                lc.lines(locale, PREFIX_COMMAND + ".component.stat",
                        "name", node.stat().render(locale, lc),
                        "key", key,
                        "nodes", chain.size()+"",
                        "chain", join(separator(text(", ", separator)),
                                chain.stream().map(n -> n.value().render(locale, lc)).collect(Collectors.toList())))
                        .ifPresent(lines::addAll);
            }

            Component hover = join(separator(newline()), lines);
            lc.lines(locale, PREFIX_COMMAND + ".component.stat_map",
                    "priority", data.priority().toString(),
                    "amount", map.size()+"",
                    "rule", data.rule().render(locale, lc))
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
        }
    }

    private Component blueprintHover(Locale locale, String command) {
        return lc.lines(locale, PREFIX_COMMAND + ".blueprint.hover",
                        "command", command)
                .map(m -> join(separator(newline()), m))
                .orElse(empty());
    }

    private ClickEvent blueprintClick(Locale locale, String command) {
        return ClickEvent.runCommand(command);
    }

    private void blueprint0(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, Component indent, int depth, PaperNode node) {
        for (var entry : node.nodes().entrySet()) {
            String key = entry.getKey();
            PaperNode child = entry.getValue();
            PaperComponent component = child.value();

            String command = "/" + rootName + " component " + component.id();
            Component hover = blueprintHover(locale, command);
            ClickEvent click = blueprintClick(locale, command);
            lc.lines(locale, PREFIX_COMMAND + ".blueprint.node",
                    "indent", Components.repeat(indent, depth),
                    "slot", node.value().slot(key).orElseThrow(IllegalStateException::new).render(locale, lc),
                    "key", key,
                    "name", component.render(locale, lc),
                    "id", component.id())
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover).clickEvent(click))));
            blueprint0(ctx, sender, locale, pSender, indent, depth + 1, child);
        }
    }

    private void blueprint(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperBlueprint blueprint = ctx.get("blueprint");

        send(sender, locale, "blueprint.header",
                "type", blueprint.getClass().getSimpleName(),
                "name", blueprint.render(locale, lc),
                "id", blueprint.id());
        blueprint.renderDescription(locale, lc).ifPresent(desc -> {
            for (var line : desc)
                send(sender, locale, "blueprint.description",
                        "line", line);
        });

        PaperNode node = blueprint.node();
        String command = "/" + rootName + " component " + node.value().id();
        Component hover = blueprintHover(locale, command);
        ClickEvent click = blueprintClick(locale, command);
        lc.lines(locale, PREFIX_COMMAND + ".blueprint.root",
                "name", node.value().render(locale, lc),
                "id", node.value().id())
                .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover).clickEvent(click))));
        blueprint0(ctx, sender, locale, pSender, lc.safe(locale, PREFIX_COMMAND + ".blueprint.indent"), 0, node);
    }
}
