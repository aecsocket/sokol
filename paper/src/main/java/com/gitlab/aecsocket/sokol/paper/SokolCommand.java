package com.gitlab.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.context.CommandContext;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.sokol.paper.command.ComponentArgument;
import com.gitlab.aecsocket.sokol.paper.impl.PaperComponent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperSlot;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

public final class SokolCommand extends BaseCommand<SokolPlugin> {
    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        manager.command(root
                .literal("component", ArgumentDescription.of("Outputs detailed information on a registered component."))
                .argument(ComponentArgument.of(plugin, "component"))
                .permission("%s.command.component".formatted(rootName))
                .handler(c -> handle(c, this::give)));
    }

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperComponent component = ctx.get("component");
        /*
              header: ${chat.prefix}${header}"<type> "${name}"<name>"${info}" (<id>):"
      tags: ${chat.prefix}${info}"  Tags: [ "${main}"<tags>"${info}" ]"
      slots: ${chat.prefix}${info}"  Slots (hover to see rule):"
      slot: ${chat.prefix}${main}"    <key>"${info}" [ <tags> ] @ <offset>"
      features: ${chat.prefix}${info}"  Features (hover to see configuration):"
      feature: ${chat.prefix}${main}"    <id>"${info}" <description>"
      stats: "TODO"
         */


        send(sender, locale, "component.header",
                "type", component.getClass().getSimpleName(),
                "name", component.name(locale),
                "id", component.id());
        send(sender, locale, "component.tags",
                "tags", String.join(", ", component.tags()));
        send(sender, locale, "component.slots",
                "amount", component.slots().size()+"");
        for (var entry : component.slots().entrySet()) {
            String key = entry.getKey();
            PaperSlot value = entry.getValue();

            lc(locale, "chat.command.component.slot",
                    "name", component.slotName(key, locale),
                    "key", key,
                    "tags", String.join(", ", value.tags()),
                    "offset", value.offset()+"")
                    .ifPresent(msg -> sender.sendMessage(msg.hoverEvent(value.rule().format())));
        }
        send(sender, locale, "component.features",
                "amount", component.featureTypes().size()+"");
    }
}
