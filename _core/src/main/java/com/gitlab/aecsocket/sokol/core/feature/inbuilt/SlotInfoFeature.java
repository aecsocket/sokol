package com.gitlab.aecsocket.sokol.core.feature.inbuilt;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.feature.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * Displays info about a node's children in item lore, by hooking into {@link ItemFeature.Events.CreateItem}.
 */
public abstract class SlotInfoFeature extends AbstractFeature {
    /** The feature ID. */
    public static final String ID = "slot_info";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public SlotInfoFeature(int listenerPriority) {
        super(listenerPriority);
    }

    /**
     * See {@link SlotInfoFeature}.
     */
    public abstract class Instance extends AbstractFeature.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract SlotInfoFeature base();

        @Override
        public void build(StatLists stats) {
            parent.events().register(ItemFeature.Events.CreateItem.class, this::event, listenerPriority);
        }

        private void addLore(Locale locale, List<Component> lore, Slot slot, @Nullable TreeNode node, Component indent, int pathLength) {
            String slotType = slot.required() ? "required"
                    : slot.internal() ? "internal" : "default";
            platform().lc().lines(locale, lck("lore"),
                    "indent", Components.repeat(indent, pathLength),
                    "slot", platform().lc().safe(locale, lck("slot." + slotType),
                            "slot", slot.name(locale)),
                    "component", node == null
                            ? platform().lc().safe(locale, lck("empty"))
                            : node.value().name(locale))
                    .ifPresent(lore::addAll);

            if (node != null) {
                for (var entry : node.slotChildren().entrySet()) {
                    addLore(locale, lore, entry.getValue().slot(), entry.getValue().child().orElse(null), indent, pathLength + 1);
                }
            }
        }

        protected void event(ItemFeature.Events.CreateItem event) {
            if (!parent.isRoot())
                return;
            List<Component> lore = new ArrayList<>();
            Component indent = platform().lc().safe(event.locale(), lck("indent"));
            for (var entry : parent.slotChildren().entrySet()) {
                addLore(event.locale(), lore, entry.getValue().slot(), entry.getValue().child().orElse(null), indent, 0);
            }
            event.item().addLore(lore);
        }
    }

    @Override public String id() { return ID; }
}
