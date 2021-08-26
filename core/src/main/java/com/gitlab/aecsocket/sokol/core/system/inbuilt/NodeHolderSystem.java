package com.gitlab.aecsocket.sokol.core.system.inbuilt;

import com.gitlab.aecsocket.minecommons.core.Quantifier;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

/**
 * Holds a list of nodes in the system, which can be taken out and placed in.
 */
public abstract class NodeHolderSystem<N extends TreeNode> extends AbstractSystem {
    /** The system ID. */
    public static final String ID = "node_holder";

    protected final int capacity;
    protected final boolean sizeAsDurability;
    protected Rule rule;

    public NodeHolderSystem(int listenerPriority, int capacity, boolean sizeAsDurability, @Nullable Rule rule) {
        super(listenerPriority);
        this.capacity = capacity;
        this.sizeAsDurability = sizeAsDurability;
        this.rule = rule;
    }

    public int capacity() { return capacity; }
    public boolean sizeAsDurability() { return sizeAsDurability; }
    public Rule rule() { return rule; }

    /**
     * See {@link NodeHolderSystem}.
     */
    public abstract class Instance extends AbstractSystem.Instance implements NodeProviderSystem {
        protected final LinkedList<Quantifier<N>> held;

        public Instance(TreeNode parent, LinkedList<Quantifier<N>> held) {
            super(parent);
            this.held = held;
        }

        public Instance(TreeNode parent) {
            this(parent, new LinkedList<>());
        }

        @Override public abstract NodeHolderSystem<N> base();

        public LinkedList<Quantifier<N>> held() { return held; }

        public boolean hasCapacity() { return capacity > 0; }
        public int size() {
            int i = 0;
            for (var qt : held)
                i += qt.amount();
            return i;
        }
        public int space() { return capacity - size(); }

        protected abstract boolean equal(N a, N b);

        public void add(N node, int amount) {
            if (held.isEmpty())
                held.add(new Quantifier<>(node, amount));
            else {
                int idx = held.size() - 1;
                var last = held.get(idx);
                if (equal(last.object(), node))
                    held.set(idx, last.add(amount));
                else
                    held.add(new Quantifier<>(node, amount));
            }
        }

        @Override
        public Optional<N> peek() {
            return held.isEmpty() ? Optional.empty() : Optional.of(held.peekLast().object());
        }

        @Override
        public Optional<N> pop() {
            return held.isEmpty() ? Optional.empty() : Optional.of(held.removeLast().object());
        }

        @Override
        public void build(StatLists stats) {
            parent.events().register(ItemSystem.Events.CreateItem.class, this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.ClickedSlotClick.class, this::event, listenerPriority);
        }

        protected void event(ItemSystem.Events.CreateItem event) {
            if (!parent.isRoot())
                return;
            Locale locale = event.locale();
            List<Component> lore = new ArrayList<>();
            for (var entry : held) {
                platform().lc().lines(locale, lck("entry"),
                        "amount", ""+entry.amount(),
                        "node", entry.object().value().name(locale))
                        .ifPresent(lore::addAll);
            }
            if (hasCapacity())
                platform().lc().lines(locale, lck("capacity"),
                        "size", ""+size(),
                        "capacity", ""+capacity)
                        .ifPresent(lore::addAll);
            event.item().addLore(lore);

            if (sizeAsDurability && hasCapacity())
                event.item().durability((double) size() / capacity);
        }

        protected void event(ItemTreeEvent.ClickedSlotClick event) {
            if (!parent.isRoot())
                return;
            event.cursor().get().ifPresentOrElse(stack -> {
                if (!event.left())
                    return;
                event.cancel();
                int rAmount = event.shift() ? 1 : stack.amount();
                int amount = hasCapacity() ? Math.min(space(), rAmount) : rAmount;
                if (amount <= 0)
                    return;
                stack.node().ifPresent(csr -> {
                    if (!rule.applies(csr))
                        return;
                    @SuppressWarnings("unchecked")
                    N nCsr = (N) csr;
                    add(nCsr, amount);
                    stack.add(-amount);
                    event.update();
                });
            }, () -> {
                if (!event.right() || !event.shift())
                    return;
                event.cancel();
                if (held.isEmpty())
                    return;
                int idx = held.size() - 1;
                var last = held.get(idx);
                event.cursor().set(last.object(), event.user().locale(), is -> is.amount(last.amount()));
                held.remove(idx);
                event.update();
            });
        }
    }

    @Override public String id() { return ID; }

    @Override
    public void loadSelf(ConfigurationNode cfg) throws SerializationException {
        rule = cfg.node("rule").get(Rule.class, Rule.Constant.TRUE);
    }
}
