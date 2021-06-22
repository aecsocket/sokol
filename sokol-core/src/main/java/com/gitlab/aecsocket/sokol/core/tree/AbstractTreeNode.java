package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractTreeNode<N extends AbstractTreeNode<N, C, S, B, Y>, C extends Component.Scoped<C, S, B>, S extends Slot, B extends System<N>, Y extends System.Instance<N>>
        implements TreeNode.Scoped<N, C, S, B, Y> {
    protected final C value;
    protected EventDispatcher<TreeEvent> events = new EventDispatcher<>();
    protected StatMap stats = new StatMap(StatMap.Priority.DEFAULT, Rule.Constant.TRUE);
    protected AtomicBoolean complete = new AtomicBoolean();
    protected final Map<String, N> children = new HashMap<>();
    protected final Map<String, Y> systems = new HashMap<>();
    protected String key;
    protected N parent;
    protected S slot;

    public AbstractTreeNode(C value) {
        this.value = value;
        for (var entry : value.baseSystems().entrySet()) {
            @SuppressWarnings("unchecked")
            Y instance = (Y) entry.getValue().create(self(), value);
            systems.put(entry.getKey(), instance);
        }
    }

    protected abstract N self();

    @Override public @NotNull C value() { return value; }
    @Override public @NotNull StatMap stats() { return stats; }
    @Override public @NotNull EventDispatcher<TreeEvent> events() { return events; }
    @Override public boolean complete() { return complete.get(); }

    @Override public @NotNull Map<String, N> children() { return children; }
    @Override public N child(String key) { return children.get(key); }
    @Override
    public @NotNull N child(String key, N child) {
        S slot = value.slot(key);
        if (slot == null)
            throw new IllegalArgumentException("Cannot add child into slot [" + key + "] to tree node without corresponding slot in component");
        if (!slot.compatible(this, child))
            throw new IllegalArgumentException("Cannot add child [" + child.value.id() + "] into slot [" + key + "] as it is not compatible");
        if (child == null)
            children.remove(key);
        else {
            children.put(key, child);
            child.parent(key, self(), slot);
        }
        return self();
    }

    @Override
    public N node(String... path) {
        if (path.length == 0)
            return self();
        N next = child(path[0]);
        if (next == null)
            return null;
        return next.node(Arrays.copyOfRange(path, 1, path.length));
    }

    @Override
    public boolean combine(N node, boolean limited) {
        AtomicBoolean success = new AtomicBoolean(false);
        visitScoped((parent, slot, child, path) -> {
            if (success.get())
                return;
            if (child == null && slot.compatible(parent, node) && (!limited || slot.fieldModifiable())) {
                success.set(true);
                parent.child(slot.key(), node);
            }
        });
        return success.get();
    }

    @Override
    public @NotNull Map<String, ChildSlot<S, N>> slotChildren() {
        Map<String, ChildSlot<S, N>> result = new HashMap<>();
        for (var entry : value.slots().entrySet()) {
            String key = entry.getKey();
            result.put(key, new ChildSlot<>(entry.getValue(), child(key)));
        }
        return result;
    }

    @Override public @NotNull Map<String, Y> systems() { return systems; }
    @Override public Y system(String id) { return systems.get(id); }
    @Override
    public void system(Y system) {
        String id = system.base().id();
        if (!value.baseSystems().containsKey(id))
            throw new IllegalArgumentException("Cannot add system [" + id + "] to tree node without corresponding base system in component");
        systems.put(id, system);
    }

    public N parent(String key, N parent, S slot) {
        this.key = key;
        this.parent = parent;
        this.slot = slot;
        if (parent != null) {
            this.events = parent.events;
            this.stats = parent.stats;
            this.complete = parent.complete;
        }
        return self();
    }

    private record StatsPair(TreeNode node, List<StatMap> stats) {}

    private void add(List<StatsPair> stats) {
        for (StatsPair pair : stats) {
            for (StatMap map : pair.stats) {
                if (map.rule().applies(pair.node)) {
                    this.stats.combineAll(map);
                }
            }
        }
    }

    protected void build0(List<StatsPair> forwardStats, List<StatsPair> reverseStats, String key, N parent, S slot) {
        StatLists stats = value.stats();
        forwardStats.add(new StatsPair(this, stats.forward()));
        reverseStats.add(0, new StatsPair(this, stats.reverse()));
        for (System.Instance<N> system : systems.values()) {
            system.build();
        }
        for (var entry : slotChildren().entrySet()) {
            N child;
            if ((child = entry.getValue().child()) == null) {
                if (entry.getValue().slot().required())
                    complete.set(false);
            } else {
                child.build0(forwardStats, reverseStats, entry.getKey(), self(), entry.getValue().slot());
            }
        }
        parent(key, parent, slot);
    }

    @Override
    public @NotNull N build() {
        events.unregisterAll();
        stats.clear();
        complete.set(true);

        List<StatsPair> forwardStats = new ArrayList<>();
        List<StatsPair> reverseStats = new ArrayList<>();

        build0(forwardStats, reverseStats, null, null, null);

        add(forwardStats);
        add(reverseStats);

        return self();
    }

    @Override
    public void visit(Visitor<TreeNode, Slot> visitor, String... path) {
        visitScoped(visitor::visit, path);
    }

    @Override
    public void visitScoped(Visitor<N, S> visitor, String... path) {
        visitor.visit(parent, slot, self(), path);
        for (var entry : slotChildren().entrySet()) {
            String[] newPath = new String[path.length + 1];
            java.lang.System.arraycopy(path, 0, newPath, 0, path.length);
            newPath[path.length] = entry.getKey();
            if (entry.getValue().child() == null)
                visitor.visit(self(), entry.getValue().slot(), null, newPath);
            else
                entry.getValue().child().visitScoped(visitor, newPath);
        }
    }

    @Override public String key() { return key; }
    @Override public N parent() { return parent; }
    @Override public S slot() { return slot; }

    @Override
    public String[] path() {
        List<String> result = new ArrayList<>();
        N current = self();
        while (current != null && current.key != null) {
            result.add(0, current.key);
            current = current.parent;
        }
        return result.toArray(new String[0]);
    }

    @Override
    public N root() {
        return parent == null ? self() : parent.root();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractTreeNode<?, ?, ?, ?, ?> that = (AbstractTreeNode<?, ?, ?, ?, ?>) o;
        return value.equals(that.value) && children.equals(that.children) && systems.equals(that.systems) && Objects.equals(key, that.key) && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, children, systems, key, parent);
    }

    @Override
    public String toString() {
        StringJoiner details = new StringJoiner(", ");
        if (systems.size() > 0)
            details.add("systems=" + systems);
        if (children.size() > 0)
            details.add("children=" + children);
        String[] path = path();
        return value.id() + ":" + (path.length == 0 ? "<root>" : String.join("/", path)) + '{' + details + '}';
    }
}
