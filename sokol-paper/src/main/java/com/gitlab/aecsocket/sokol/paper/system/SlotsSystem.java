package com.gitlab.aecsocket.sokol.paper.system;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.stat.Descriptor;
import com.gitlab.aecsocket.sokol.paper.stat.SoundsStat;
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
            .put("combine_sound", new SoundsStat())
            .put("insert_sound", new SoundsStat())
            .put("remove_sound", new SoundsStat())
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
            parent.events().register(Events.InsertInto.class, this::event);
            parent.events().register(Events.RemoveFrom.class, this::event);
        }

        private void event(Events.CombineNodeOntoParent event) {
            if (!parent.isRoot())
                return;
            Location location = event.handle.getWhoClicked().getLocation();
            parent.stats().<Descriptor<List<PreciseSound>>>value("combine_sound")
                    .ifPresent(d -> d.value().forEach(s -> s.play(platform, location)));
        }

        private void event(Events.InsertInto event) {
            if (!parent.isRoot())
                return;
            Location location = event.handle.getWhoClicked().getLocation();
            parent.stats().<Descriptor<List<PreciseSound>>>value("insert_sound")
                    .ifPresent(d -> d.value().forEach(s -> s.play(platform, location)));
        }

        private void event(Events.RemoveFrom event) {
            if (!parent.isRoot())
                return;
            Location location = event.handle.getWhoClicked().getLocation();
            // create our own root because, at this point, the removing node is still attached to the parent
            // so it will use its parent's stats
            parent.asRoot().stats().<Descriptor<List<PreciseSound>>>value("remove_sound")
                    .ifPresent(d -> d.value().forEach(s -> s.play(platform, location)));
        }
    }

    private final SokolPlugin platform;

    public SlotsSystem(SokolPlugin platform) {
        this.platform = platform;
    }

    @Override public @NotNull String id() { return ID; }

    public SokolPlugin platform() { return platform; }

    @Override public @NotNull Map<String, Stat<?>> statTypes() { return STATS; }

    @Override
    public @NotNull Instance create(TreeNode node) {
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
            private final InventoryClickEvent handle;
            private final PaperTreeNode node;
            private final PaperTreeNode parent;
            private boolean cancelled;

            public CombineNodeOntoParent(InventoryClickEvent handle, PaperTreeNode node, PaperTreeNode parent) {
                this.handle = handle;
                this.node = node;
                this.parent = parent;
            }

            public InventoryClickEvent handle() { return handle; }
            @Override public PaperTreeNode node() { return node; }
            public PaperTreeNode parent() { return parent; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class CombineChildOntoNode implements TreeEvent, Cancellable {
            private final InventoryClickEvent handle;
            private final PaperTreeNode node;
            private final PaperTreeNode child;
            private boolean cancelled;

            public CombineChildOntoNode(InventoryClickEvent handle, PaperTreeNode node, PaperTreeNode child) {
                this.handle = handle;
                this.node = node;
                this.child = child;
            }

            public InventoryClickEvent handle() { return handle; }
            @Override public PaperTreeNode node() { return node; }
            public PaperTreeNode child() { return child; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class SlotModify implements TreeEvent, Cancellable {
            private final InventoryClickEvent handle;
            private final PaperTreeNode node;
            private final PaperSlot slot;
            private final PaperTreeNode oldChild;
            private PaperTreeNode newChild;
            private boolean cancelled;

            public SlotModify(InventoryClickEvent handle, PaperTreeNode node, PaperSlot slot, PaperTreeNode oldChild, PaperTreeNode newChild) {
                this.handle = handle;
                this.node = node;
                this.slot = slot;
                this.oldChild = oldChild;
                this.newChild = newChild;
            }

            public InventoryClickEvent handle() { return handle; }
            @Override public PaperTreeNode node() { return node; }
            public PaperSlot slot() { return slot; }
            public PaperTreeNode oldChild() { return oldChild; }

            public PaperTreeNode newChild() { return newChild; }
            public void newChild(PaperTreeNode newChild) { this.newChild = newChild; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class InsertInto implements TreeEvent, Cancellable {
            private final InventoryClickEvent handle;
            private final PaperTreeNode node;
            private final PaperTreeNode parent;
            private final PaperSlot slot;
            private final PaperTreeNode replacing;
            private boolean cancelled;

            public InsertInto(InventoryClickEvent handle, PaperTreeNode node, PaperTreeNode parent, PaperSlot slot, PaperTreeNode replacing) {
                this.handle = handle;
                this.node = node;
                this.parent = parent;
                this.slot = slot;
                this.replacing = replacing;
            }

            public InventoryClickEvent handle() { return handle; }
            @Override public PaperTreeNode node() { return node; }
            public PaperTreeNode parent() { return parent; }
            public PaperSlot slot() { return slot; }
            public PaperTreeNode replacing() { return replacing; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class RemoveFrom implements TreeEvent, Cancellable {
            private final InventoryClickEvent handle;
            private final PaperTreeNode node;
            private final PaperTreeNode parent;
            private final PaperSlot slot;
            private final PaperTreeNode replacement;
            private boolean cancelled;

            public RemoveFrom(InventoryClickEvent handle, PaperTreeNode node, PaperTreeNode parent, PaperSlot slot, PaperTreeNode replacement) {
                this.handle = handle;
                this.node = node;
                this.parent = parent;
                this.slot = slot;
                this.replacement = replacement;
            }

            public InventoryClickEvent handle() { return handle; }
            @Override public PaperTreeNode node() { return node; }
            public PaperTreeNode parent() { return parent; }
            public PaperSlot slot() { return slot; }
            public PaperTreeNode replacement() { return replacement; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }
    }
}
