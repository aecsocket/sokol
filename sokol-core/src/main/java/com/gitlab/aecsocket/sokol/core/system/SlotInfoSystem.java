package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.tree.ScopedTreeNode;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class SlotInfoSystem<N extends ScopedTreeNode<N, ?, ?, ?, ?>> extends AbstractSystem<N> {
    public static final String ID = "slot_info";

    public static abstract class Instance<N extends ScopedTreeNode<N, ?, ?, ?, ?>> extends AbstractSystem.Instance<N> {
        public Instance(N parent) {
            super(parent);
        }

        @Override
        public void build() {
            parent.events().register(ItemSystem.Events.CreateItem.class, this::event);
        }

        private void addLore(Locale locale, List<Component> lore, Slot slot, N node, Component indent, int pathLength) {
            String slotType = slot.required() ? "required"
                    : slot.internal() ? "internal" : "default";
            lore.add(Components.BLANK.append(platform().localize(locale, "system.slot_info.lore",
                    "indent", Components.repeat(indent, pathLength),
                    "slot", platform().localize(locale, "system.slot_info." + slotType,
                            "slot", slot.name(locale)),
                    "component", node == null ? platform().localize(locale, "system.slot_info.empty") : node.value().name(locale))));

            if (node != null) {
                for (var entry : node.slotChildren().entrySet()) {
                    addLore(locale, lore, entry.getValue().slot(), entry.getValue().child(), indent, pathLength + 1);
                }
            }
        }

        private void event(ItemSystem.Events.CreateItem event) {
            if (!parent.isRoot())
                return;
            List<Component> lore = new ArrayList<>();
            Component indent = platform().localize(event.locale(), "system.slot_info.indent");
            for (var entry : parent.slotChildren().entrySet()) {
                addLore(event.locale(), lore, entry.getValue().slot(), entry.getValue().child(), indent, 0);
            }
            event.item().addLore(lore);
        }
    }

    @Override public @NotNull String id() { return ID; }
    @Override public @NotNull Map<String, Stat<?>> baseStats() { return Collections.emptyMap(); }
}
