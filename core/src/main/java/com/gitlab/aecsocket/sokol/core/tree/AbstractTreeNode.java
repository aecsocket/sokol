package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatMap;
import com.gitlab.aecsocket.sokol.core.stat.StatOperationException;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract tree node with default implementations for methods.
 * @param <N> The type of this node.
 * @param <C> The component type.
 * @param <S> The slot type.
 * @param <B> The base system type.
 * @param <Y> The system instance type.
 */
public abstract class AbstractTreeNode<N extends AbstractTreeNode<N, C, S, B, Y>, C extends Component.Scoped<C, S, B>, S extends Slot, B extends System, Y extends System.Instance>
        implements TreeNode.Scoped<N, C, S, B, Y> {
    protected final C value;
    protected EventDispatcher<TreeEvent> events = new EventDispatcher<>();
    protected StatMap stats = new StatMap(StatMap.Priority.DEFAULT, Rule.Constant.TRUE);
    protected AtomicBoolean complete = new AtomicBoolean();
    protected final Map<String, N> children = new HashMap<>();
    protected final Map<String, Y> systems = new HashMap<>();
    protected N parent;
    protected S slot;

    public AbstractTreeNode(C value) {
        this.value = value;
        for (var entry : value.baseSystems().entrySet()) {
            @SuppressWarnings("unchecked")
            Y instance = (Y) entry.getValue().create(self());
            systems.put(entry.getKey(), instance);
        }
    }

    /**
     * Returns this instance as a {@link N}.
     * @return This instance.
     */
    protected abstract N self();

    @Override public C value() { return value; }
    @Override public StatMap stats() { return stats; }
    @Override public EventDispatcher<TreeEvent> events() { return events; }
    @Override public boolean complete() { return complete.get(); }

    @Override public Map<String, N> children() { return children; }
    @Override public Optional<N> child(String key) { return Optional.ofNullable(children.get(key)); }
    @Override public void removeChild(String key) { children.remove(key); }

    @Override
    public void child(String key, @Nullable N child) throws IllegalArgumentException {
        S slot = value.slot(key)
                .orElseThrow(() -> new IllegalArgumentException("Cannot add child into slot [" + key + "] to tree node without corresponding slot in component"));
        if (!slot.compatible(this, child))
            throw new IllegalArgumentException("Cannot add child [" + child.value.id() + "] into slot [" + key + "] as it is not compatible");
        if (child == null)
            children.remove(key);
        else {
            children.put(key, child);
            child.parent(self(), slot);
        }
    }

    @Override
    public Optional<N> node(String... path) {
        if (path.length == 0)
            return Optional.of(self());
        return child(path[0])
                .flatMap(next -> next.node(Arrays.copyOfRange(path, 1, path.length)));
    }

    @Override
    public N combine(TreeNode node, boolean limited) {
        @SuppressWarnings("unchecked")
        N nNode = (N) node;
        AtomicReference<N> result = new AtomicReference<>();
        this.visitSlotsScoped((parent, slot, child, path) -> {
            if (result.get() != null)
                return;
            if (child == null && slot.compatible(parent, node) && (!limited || slot.fieldModifiable())) {
                result.set(parent);
                parent.child(slot.key(), nNode);
            }
        });
        return result.get();
    }

    @Override
    public Map<String, ChildSlot<S, N>> slotChildren() {
        Map<String, ChildSlot<S, N>> result = new HashMap<>();
        for (var entry : value.slots().entrySet()) {
            String key = entry.getKey();
            result.put(key, new ChildSlot<>(entry.getValue(), child(key)));
        }
        return result;
    }

    @Override public Map<String, Y> systems() { return systems; }
    @SuppressWarnings("unchecked")
    @Override public <T extends System.Instance> Optional<T> system(System.Key<T> key) { return Optional.ofNullable((T) systems.get(key.id())); }
    @Override public Optional<? extends System.Instance> system(String id) { return Optional.ofNullable(systems.get(id)); }
    @Override
    public void system(Y system) throws IllegalArgumentException {
        String id = system.base().id();
        if (!value.baseSystems().containsKey(id))
            throw new IllegalArgumentException("Cannot add system [" + id + "] to tree node without corresponding base system in component");
        systems.put(id, system);
    }

    @Override
    public <T extends System.Instance> Optional<T> system(Class<T> type) {
        for (System.Instance system : systems.values()) {
            if (type.isInstance(system))
                return Optional.of(type.cast(system));
        }
        return Optional.empty();
    }

    /**
     * Parents this node to a parent node.
     * <p>
     * Note: If {@code parent} is null, then {@code slot} must be null as well, and vice versa.
     * @param parent The parent node, or null if this node should become the root.
     * @param slot The parent slot, or null if this node should become the root.
     * @return This instance.
     */
    public N parent(@Nullable N parent, @Nullable S slot) {
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
        int i = 0;
        for (StatsPair pair : stats) {
            int j = 0;
            for (StatMap map : new ArrayList<>(pair.stats)) {
                if (map.rule().applies(pair.node)) {
                    for (var entry : map.entrySet()) {
                        try {
                            this.stats.chain(entry.getValue());
                        } catch (StatOperationException e) {
                            throw new IllegalArgumentException("Could not combine stat [" + entry.getKey() + "] of pair " + i + " map " + j, e);
                        }
                    }
                }
                ++j;
            }
            ++i;
        }
    }

    protected void build0(List<StatsPair> forwardStats, List<StatsPair> reverseStats, @Nullable N parent, @Nullable S slot) {
        StatLists stats = new StatLists(value.stats());
        for (System.Instance system : systems.values()) {
            system.build(stats);
        }
        forwardStats.add(new StatsPair(this, stats.forward()));
        reverseStats.add(0, new StatsPair(this, stats.reverse()));
        for (var entry : slotChildren().entrySet()) {
            entry.getValue().child().ifPresentOrElse(
                    child -> child.build0(forwardStats, reverseStats, self(), entry.getValue().slot()),() -> {
                if (entry.getValue().slot().required())
                    complete.set(false);
            });
        }
        parent(parent, slot);
    }

    @Override
    public N build() throws IllegalArgumentException {
        events.unregisterAll();
        stats.clear();
        complete.set(true);

        List<StatsPair> forwardStats = new ArrayList<>();
        List<StatsPair> reverseStats = new ArrayList<>();

        build0(forwardStats, reverseStats, null, null);

        add(forwardStats);
        add(reverseStats);

        return self();
    }

    @Override
    public void visitNodes(NodeVisitor<TreeNode> visitor, String... path) {
        visitNodesScoped(visitor::visit, path);
    }

    @Override
    public void visitNodesScoped(NodeVisitor<N> visitor, String... path) {
        visitor.visit(self(), path);
        for (var entry : slotChildren().entrySet()) {
            String[] newPath = new String[path.length + 1];
            java.lang.System.arraycopy(path, 0, newPath, 0, path.length);
            newPath[path.length] = entry.getKey();
            entry.getValue().child().ifPresent(child -> child.visitNodesScoped(visitor, newPath));
        }
    }

    @Override
    public void visitSlots(SlotVisitor<TreeNode, Slot> visitor, String... path) {
        visitSlotsScoped(visitor::visit, path);
    }

    @Override
    public void visitSlotsScoped(SlotVisitor<N, S> visitor, String... path) {
        for (var entry : slotChildren().entrySet()) {
            String[] newPath = new String[path.length + 1];
            java.lang.System.arraycopy(path, 0, newPath, 0, path.length);
            newPath[path.length] = entry.getKey();
            visitor.visit(self(), entry.getValue().slot(), entry.getValue().child().orElse(null), newPath);
            entry.getValue().child().ifPresent(child -> child.visitSlotsScoped(visitor, newPath));
        }
    }

    @Override public Optional<N> parent() { return Optional.ofNullable(parent); }
    @Override public Optional<S> slot() { return Optional.ofNullable(slot); }

    @Override
    public String[] path() {
        List<String> result = new ArrayList<>();
        N current = self();
        while (current != null && current.slot != null) {
            result.add(0, current.slot.key());
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
        return value.equals(that.value) && children.equals(that.children) && systems.equals(that.systems) && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, children, systems);
    }

    @Override
    public String toString() {
        StringJoiner details = new StringJoiner(", ");
        if (children.size() > 0)
            details.add("children=" + children);
        String[] path = path();
        return String.join("/", path) + "/" + value.id() + '{' + details + '}';
    }
}
