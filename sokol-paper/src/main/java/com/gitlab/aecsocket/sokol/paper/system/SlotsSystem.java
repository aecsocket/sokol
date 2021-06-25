package com.gitlab.aecsocket.sokol.paper.system;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.stat.Descriptor;
import com.gitlab.aecsocket.sokol.paper.stat.SoundsStat;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlotsSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "slots";
    public static final PaperSystem.Type TYPE = (plugin, node) -> new SlotsSystem(plugin);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("combine_onto_sound", new SoundsStat())
            .put("combine_from_sound", new SoundsStat())
            .build();

    public final class Instance extends AbstractSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public @NotNull SlotsSystem base() { return SlotsSystem.this; }
        @Override public @NotNull SokolPlugin platform() { return platform; }

        @Override
        public void build() {
            parent.events().register(Events.CombineNodeOntoParent.class, this::event);
            parent.events().register(Events.CombineChildOntoNode.class, this::event);
            parent.events().register(Events.SlotA.class, this::event);
            parent.events().register(Events.SlotB.class, this::event);
        }

        private void event(Events.CombineNodeOntoParent event) {
            if (!parent.isRoot())
                return;
            Location location = event.handle.getWhoClicked().getLocation();
            parent.stats().<Descriptor<List<PreciseSound>>>optValue("combine_onto_sound")
                    .ifPresent(d -> d.value().forEach(s -> s.play(platform, location)));
        }

        private void event(Events.CombineChildOntoNode event) {
            if (!parent.isRoot())
                return;
            Location location = event.handle.getWhoClicked().getLocation();
            parent.stats().<Descriptor<List<PreciseSound>>>optValue("combine_from_sound")
                    .ifPresent(d -> d.value().forEach(s -> s.play(platform, location)));
        }

        private void event(Events.SlotA event) {
            if (!parent.isRoot())
                return;
            System.out.println("slotA = " + new ReflectionToStringBuilder(event));
        }

        private void event(Events.SlotB event) {
            if (!parent.isRoot())
                return;
            System.out.println("slotB = " + new ReflectionToStringBuilder(event));
        }
    }

    private final SokolPlugin platform;

    public SlotsSystem(SokolPlugin platform) {
        this.platform = platform;
    }

    @Override public @NotNull String id() { return ID; }

    public SokolPlugin platform() { return platform; }

    @Override public @NotNull Map<String, Stat<?>> baseStats() { return STATS; }

    @Override
    public @NotNull Instance create(TreeNode node, Component component) {
        return new Instance(node);
    }

    @Override
    public @NotNull Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public @NotNull Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode config) throws SerializationException {
        return new Instance(node);
    }

    public static final class Events {
        private Events() {}

        public static final class CombineNodeOntoParent implements TreeEvent, Cancellable {
            private final PaperTreeNode node;
            private final PaperTreeNode parent;
            private final InventoryClickEvent handle;
            private boolean cancelled;

            public CombineNodeOntoParent(PaperTreeNode node, PaperTreeNode parent, InventoryClickEvent handle) {
                this.node = node;
                this.parent = parent;
                this.handle = handle;
            }

            @Override public PaperTreeNode node() { return node; }
            public PaperTreeNode parent() { return parent; }
            public InventoryClickEvent handle() { return handle; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class CombineChildOntoNode implements TreeEvent, Cancellable {
            private final PaperTreeNode node;
            private final PaperTreeNode child;
            private final InventoryClickEvent handle;
            private boolean cancelled;

            public CombineChildOntoNode(PaperTreeNode node, PaperTreeNode child, InventoryClickEvent handle) {
                this.node = node;
                this.child = child;
                this.handle = handle;
            }

            @Override public PaperTreeNode node() { return node; }
            public PaperTreeNode child() { return child; }
            public InventoryClickEvent handle() { return handle; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class SlotA implements TreeEvent, Cancellable {
            private final PaperTreeNode node;
            private final PaperSlot slot;
            private final PaperTreeNode child;
            private boolean cancelled;

            public SlotA(PaperTreeNode node, PaperSlot slot, PaperTreeNode child) {
                this.node = node;
                this.slot = slot;
                this.child = child;
            }

            @Override public PaperTreeNode node() { return node; }
            public PaperSlot slot() { return slot; }
            public PaperTreeNode child() { return child; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class SlotB implements TreeEvent, Cancellable {
            private final PaperTreeNode node;
            private final PaperSlot slot;
            private final PaperTreeNode child;
            private boolean cancelled;

            public SlotB(PaperTreeNode node, PaperSlot slot, PaperTreeNode child) {
                this.node = node;
                this.slot = slot;
                this.child = child;
            }

            @Override public PaperTreeNode node() { return node; }
            public PaperSlot slot() { return slot; }
            public PaperTreeNode child() { return child; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }
    }
}
