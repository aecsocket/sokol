package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Displays info about a node's children in item lore, by hooking into {@link ItemSystem.Events.CreateItem}.
 */
public abstract class SlotInfoSystem extends AbstractSystem {
    /** The system ID. */
    public static final String ID = "slot_info";

    /**
     * See {@link SlotInfoSystem}.
     */
    public static abstract class Instance extends AbstractSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override
        public void build() {
            parent.events().register(ItemSystem.Events.CreateItem.class, this::event);
        }

        private void addLore(Locale locale, List<Component> lore, Slot slot, TreeNode node, Component indent, int pathLength) {
            String slotType = slot.required() ? "required"
                    : slot.internal() ? "internal" : "default";
            lore.add(Components.BLANK.append(localize(locale, "lore",
                    "indent", Components.repeat(indent, pathLength),
                    "slot", localize(locale, slotType,
                            "slot", slot.name(locale)),
                    "component", node == null ? localize(locale, "empty") : node.value().name(locale))));

            if (node != null) {
                for (var entry : node.slotChildren().entrySet()) {
                    addLore(locale, lore, entry.getValue().slot(), entry.getValue().child().orElse(null), indent, pathLength + 1);
                }
            }
        }

        private void event(ItemSystem.Events.CreateItem event) {
            if (!parent.isRoot())
                return;
            List<Component> lore = new ArrayList<>();
            Component indent = localize(event.locale(), "indent");
            for (var entry : parent.slotChildren().entrySet()) {
                addLore(event.locale(), lore, entry.getValue().slot(), entry.getValue().child().orElse(null), indent, 0);
            }
            event.item().addLore(lore);
        }
    }

    @Override public @NotNull String id() { return ID; }
}
