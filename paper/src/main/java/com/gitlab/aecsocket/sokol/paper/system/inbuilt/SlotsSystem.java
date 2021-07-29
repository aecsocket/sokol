package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.*;
import com.gitlab.aecsocket.sokol.paper.slotview.SlotViewPane;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.Location;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;

public class SlotsSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "slots";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("combine_sound", soundsStat(null))
            .put("insert_sound", soundsStat(null))
            .put("remove_sound", soundsStat(null))
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofStats(STATS);

    public final class Instance extends AbstractSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public SlotsSystem base() { return SlotsSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        public void build(StatLists stats) {
            parent.events().register(Events.CombineNodeOntoParent.class, this::event);
            parent.events().register(Events.InsertInto.class, this::event);
            parent.events().register(Events.RemoveFrom.class, this::event);
            parent.events().register(PaperEvent.ClickedSlotClick.class, this::event);
        }

        protected void event(Events.CombineNodeOntoParent event) {
            if (!parent.isRoot())
                return;
            Location location = event.handle.getWhoClicked().getLocation();
            parent.stats().<List<PreciseSound>>desc("combine_sound")
                    .ifPresent(v -> v.forEach(s -> s.play(platform, location)));
        }

        protected void event(Events.InsertInto event) {
            if (!parent.isRoot())
                return;
            Location location = event.handle.getWhoClicked().getLocation();
            parent.stats().<List<PreciseSound>>desc("insert_sound")
                    .ifPresent(v -> v.forEach(s -> s.play(platform, location)));
        }

        protected void event(Events.RemoveFrom event) {
            if (!parent.isRoot())
                return;
            Location location = event.handle.getWhoClicked().getLocation();
            // create our own root because, at this point, the removing node is still attached to the parent
            // so it will use its parent's stats
            event.node.asRoot().stats().<List<PreciseSound>>desc("remove_sound")
                    .ifPresent(v -> v.forEach(s -> s.play(platform, location)));
        }

        protected void event(PaperEvent.ClickedSlotClick event) {
            if (!parent.isRoot())
                return;
            InventoryClickEvent handle = event.handle();
            Locale locale = event.user().locale();
            PaperTreeNode root = event.node();

            ItemStack clickedStack = event.slot().paperGet();
            ItemStack cursorStack = event.cursor().paperGet();

            // Combining
            if (handle.getClick() == ClickType.LEFT && combine && !PaperUtils.empty(cursorStack)) {
                platform.persistenceManager().load(cursorStack).ifPresent(cursor -> {
                    PaperTreeNode oldCursor = cursor.asRoot();
                    if (parent.root().combine(cursor, combineLimited) != null) {
                        if (
                                new Events.CombineChildOntoNode(handle, root, cursor).call()
                                | new Events.CombineNodeOntoParent(handle, oldCursor, root).call()
                        ) return;

                        int cursorAmount = cursorStack.getAmount();
                        int clickedAmount = clickedStack.getAmount();
                        if (cursorAmount >= clickedAmount) {
                            event.update(is -> is.amount(clickedAmount));
                            cursorStack.subtract(clickedAmount);
                        } else {
                            event.cursor().set(root, locale, is -> is.amount(cursorAmount));
                            clickedStack.subtract(cursorAmount);
                        }
                    }
                });
                return;
            }

            // Slot view
            if (slotView && handle.getClick() == ClickType.RIGHT && PaperUtils.empty(handle.getCursor())) {
                handle.getView().setCursor(null);
                int clickedSlot = handle.getSlot();
                boolean clickedTop = handle.getClickedInventory() == handle.getView().getTopInventory();
                event.cancel();
                platform.guis()
                        .create(new SlotViewPane(platform, 9, 6, locale, root)
                                .modification(clickedStack.getAmount() == 1 && slotViewModification) // todo
                                .limited(slotViewLimited)
                                .treeModifyCallback(node ->
                                        handle.setCurrentItem(node.system(PaperItemSystem.KEY).orElseThrow().create(locale).handle())), evt -> {
                            if (Guis.isInvalid(evt, clickedTop, clickedSlot))
                                evt.setCancelled(true);
                        })
                        .show(handle.getWhoClicked());
            }
        }
    }

    private final SokolPlugin platform;
    private final boolean slotView;
    private final boolean slotViewModification;
    private final boolean slotViewLimited;
    private final boolean combine;
    private final boolean combineLimited;

    public SlotsSystem(SokolPlugin platform, int listenerPriority, boolean slotView, boolean slotViewModification, boolean slotViewLimited, boolean combine, boolean combineLimited) {
        super(listenerPriority);
        this.platform = platform;
        this.slotView = slotView;
        this.slotViewModification = slotViewModification;
        this.slotViewLimited = slotViewLimited;
        this.combine = combine;
        this.combineLimited = combineLimited;
    }

    @Override public String id() { return ID; }

    public SokolPlugin platform() { return platform; }
    public boolean slotView() { return slotView; }
    public boolean slotViewModification() { return slotViewModification; }
    public boolean slotViewLimited() { return slotViewLimited; }
    public boolean combine() { return combine; }
    public boolean combineLimited() { return combineLimited; }

    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node);
    }

    public static ConfigType type(SokolPlugin platform) {
        return cfg -> new SlotsSystem(platform,
                cfg.node(keyListenerPriority).getInt(),
                cfg.node("slot_view").getBoolean(true),
                cfg.node("slot_view_modification").getBoolean(true),
                cfg.node("slot_view_limited").getBoolean(true),
                cfg.node("combine").getBoolean(true),
                cfg.node("combine_limited").getBoolean(true));
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
            private final @Nullable PaperTreeNode oldChild;
            private @Nullable PaperTreeNode newChild;
            private boolean cancelled;

            public SlotModify(InventoryClickEvent handle, PaperTreeNode node, PaperSlot slot, @Nullable PaperTreeNode oldChild, @Nullable PaperTreeNode newChild) {
                this.handle = handle;
                this.node = node;
                this.slot = slot;
                this.oldChild = oldChild;
                this.newChild = newChild;
            }

            public InventoryClickEvent handle() { return handle; }
            @Override public PaperTreeNode node() { return node; }
            public PaperSlot slot() { return slot; }
            public Optional<PaperTreeNode> oldChild() { return Optional.ofNullable(oldChild); }

            public Optional<PaperTreeNode> newChild() { return Optional.ofNullable(newChild); }
            public void newChild(PaperTreeNode newChild) { this.newChild = newChild; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class InsertInto implements TreeEvent, Cancellable {
            private final InventoryClickEvent handle;
            private final PaperTreeNode node;
            private final PaperTreeNode parent;
            private final PaperSlot slot;
            private final @Nullable PaperTreeNode replacing;
            private boolean cancelled;

            public InsertInto(InventoryClickEvent handle, PaperTreeNode node, PaperTreeNode parent, PaperSlot slot, @Nullable PaperTreeNode replacing) {
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
            public Optional<PaperTreeNode> replacing() { return Optional.ofNullable(replacing); }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class RemoveFrom implements TreeEvent, Cancellable {
            private final InventoryClickEvent handle;
            private final PaperTreeNode node;
            private final PaperTreeNode parent;
            private final PaperSlot slot;
            private final @Nullable PaperTreeNode replacement;
            private boolean cancelled;

            public RemoveFrom(InventoryClickEvent handle, PaperTreeNode node, PaperTreeNode parent, PaperSlot slot, @Nullable PaperTreeNode replacement) {
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
            public Optional<PaperTreeNode> replacement() { return Optional.ofNullable(replacement); }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }
    }
}
