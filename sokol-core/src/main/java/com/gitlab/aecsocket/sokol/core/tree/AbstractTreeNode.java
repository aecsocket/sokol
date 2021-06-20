package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractTreeNode<N extends AbstractTreeNode<N, C, S, B, Y>, C extends Component.Scoped<C, S, B>, S extends Slot, B extends System<N>, Y extends System.Instance<N>>
        implements ScopedTreeNode<N, C, S, B, Y> {
    protected final C value;
    protected final EventDispatcher<TreeEvent> events;
    protected final StatMap stats;
    protected final Map<String, N> children = new HashMap<>();
    protected final Map<String, Y> systems = new HashMap<>();
    protected String key;
    protected N parent;
    protected S slot;

    public AbstractTreeNode(C value, EventDispatcher<TreeEvent> events, StatMap stats, String key, N parent) {
        this.value = value;
        this.events = events;
        this.stats = stats;
        this.key = key;
        this.parent = parent;
        for (var entry : value.baseSystems().entrySet()) {
            @SuppressWarnings("unchecked")
            Y instance = (Y) entry.getValue().create(self(), value);
            systems.put(entry.getKey(), instance);
        }
        slot = parent != null ? parent.value.slot(key) : null;
    }

    public AbstractTreeNode(C value, EventDispatcher<TreeEvent> events, StatMap stats) {
        this(value, events, stats, null, null);
    }

    public AbstractTreeNode(C value, N o) {
        this(value, o.events, o.stats);
    }

    public AbstractTreeNode(C value) {
        this(value, new EventDispatcher<>(), new StatMap(StatMap.Priority.DEFAULT));
    }

    protected abstract N self();

    @Override public @NotNull C value() { return value; }
    @Override public @NotNull StatMap stats() { return stats; }
    @Override public @NotNull EventDispatcher<TreeEvent> events() { return events; }

    @Override public @NotNull Map<String, N> children() { return children; }
    @Override public N child(String key) { return children.get(key); }
    @Override
    public @NotNull N child(String key, N child) {
        S slot = value.slot(key);
        if (slot == null)
            throw new IllegalArgumentException("Cannot add child into slot [" + key + "] to tree node without corresponding slot in component");
        if (!slot.compatible(child))
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

    public void parent(String key, N parent, S slot) {
        this.key = key;
        this.parent = parent;
        this.slot = slot;
    }

    private void add(List<List<StatMap>> stats) {
        for (List<StatMap> statsList : stats) {
            for (StatMap map : statsList) {
                this.stats.combineAll(map);
            }
        }
    }

    protected void build0(List<List<StatMap>> forwardStats, List<List<StatMap>> reverseStats, String key, N parent, S slot) {
        StatLists stats = value.stats();
        forwardStats.add(stats.forward());
        reverseStats.add(0, stats.reverse());
        for (System.Instance<N> system : systems.values()) {
            system.build();
        }
        this.key = key;
        this.parent = parent;
        this.slot = slot;
    }

    @Override
    public @NotNull N build() {
        events.unregisterAll();
        stats.clear();

        List<List<StatMap>> forwardStats = new ArrayList<>();
        List<List<StatMap>> reverseStats = new ArrayList<>();

        build0(forwardStats, reverseStats, null, null, null);
        for (var entry : slotChildren().entrySet()) {
            N child;
            if ((child = entry.getValue().child()) != null) {
                child.build0(forwardStats, reverseStats, entry.getKey(), self(), entry.getValue().slot());
            }
        }

        add(forwardStats);
        add(reverseStats);

        return self();
    }

    @Override
    public void visit(Visitor<TreeNode> visitor, String... path) {
        visitScoped(visitor::visit, path);
    }

    @Override
    public void visitScoped(Visitor<N> visitor, String... path) {
        visitor.visit(self(), path);
        for (var entry : children.entrySet()) {
            String[] newPath = new String[path.length + 1];
            java.lang.System.arraycopy(path, 0, newPath, 0, path.length);
            newPath[path.length] = entry.getKey();
            entry.getValue().visitScoped(visitor, newPath);
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
