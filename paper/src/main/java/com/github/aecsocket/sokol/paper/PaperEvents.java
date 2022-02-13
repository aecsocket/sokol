package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.minecommons.core.InputType;
import com.github.aecsocket.minecommons.core.event.Cancellable;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.inputs.Inputs;
import com.github.aecsocket.sokol.core.event.ItemEvent;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.paper.context.PaperContext;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import com.github.aecsocket.sokol.paper.world.slot.PaperItemSlot;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Function;

public final class PaperEvents {
    private PaperEvents() {}

    public record CreateItem(
        PaperTreeNode node,
        PaperItemStack item
    ) implements NodeEvent.CreateItem<PaperTreeNode, PaperBlueprintNode, PaperItemStack> {}

    public record Hold(PaperTreeNode node) implements ItemEvent.Hold<PaperTreeNode> {}

    public static abstract class Base implements NodeEvent<PaperTreeNode> {
        protected final PaperTreeNode node;

        public Base(PaperTreeNode node) {
            this.node = node;
        }

        @Override public PaperTreeNode node() { return node; }
    }

    private static abstract class BaseCancellable extends Base implements Cancellable {
        private boolean cancelled;

        public BaseCancellable(PaperTreeNode node) {
            super(node);
        }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
    }

    public static final class Input extends BaseCancellable implements ItemEvent.Input<PaperTreeNode> {
        private final Inputs.Events.Input backing;

        public Input(PaperTreeNode node, Inputs.Events.Input backing) {
            super(node);
            this.backing = backing;
        }

        public Inputs.Events.Input backing() { return backing; }
        @Override public InputType input() { return backing.input(); }
    }

    public static final class GameClick extends BaseCancellable implements ItemEvent.GameClick<PaperTreeNode> {
        private final PlayerInteractEvent backing;
        private final @Nullable Vector3 clickedPos;

        public GameClick(PaperTreeNode node, PlayerInteractEvent backing) {
            super(node);
            this.backing = backing;
            clickedPos = backing.getInteractionPoint() == null ? null : PaperUtils.toCommons(backing.getInteractionPoint());
        }

        public PlayerInteractEvent backing() { return backing; }
        @Override public Optional<Vector3> clickedPos() { return Optional.ofNullable(clickedPos); }
    }

    public static void forInventory(SokolPlugin plugin, Player player, Function<PaperTreeNode, NodeEvent<PaperTreeNode>> eventFunction) {
        var user = PaperItemUser.user(plugin, player);
        PlayerInventory inv = player.getInventory();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = inv.getItem(slot);
            plugin.persistence().load(item).ifPresent(bp ->
                bp.asTreeNode(PaperContext.context(
                    user,
                    new PaperItemStack(plugin, item),
                    PaperItemSlot.itemSlot(plugin, player, slot)
                )).tree().andCall(eventFunction)
            );
        }
    }
}
