package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.node.RuleException;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public abstract class AbstractNode<
        N extends AbstractNode<N, C, F>,
        C extends Component.Scoped<C, ?, ? extends Feature<? extends F, N>, N>,
        F extends FeatureInstance<N>
> implements Node.Scoped<N, C, F> {
    protected record NodeKey<N extends Node>(N parent, String key) {}

    protected final C value;
    protected @Nullable NodeKey<N> key;
    protected TreeData.Scoped<N> treeData;
    protected final Map<String, N> nodes = new HashMap<>();
    protected final Map<String, ? extends F> features;

    protected AbstractNode(C value, @Nullable NodeKey<N> key, Map<String, ? extends F> features, TreeData.@Nullable Scoped<N> treeData) {
        this.value = value;
        this.key = key;
        this.features = features;
        this.treeData = treeData;
    }

    protected AbstractNode(C value, @Nullable NodeKey<N> key, Map<String, ? extends F> features) {
        this(value, key, features, null);
    }

    protected AbstractNode(C value, @Nullable NodeKey<N> key) {
        this.value = value;
        this.key = key;
        Map<String, F> features = new HashMap<>();
        for (var entry : value.features().entrySet()) {
            F feature = entry.getValue().create(self());
            features.put(entry.getKey(), feature);
        }
        this.features = features;
    }

    public AbstractNode(C value, @Nullable N parent, @Nullable String key) {
        this(value, parent == null || key == null ? null : new NodeKey<>(parent, key));
    }

    public AbstractNode(C value) {
        this(value, null);
    }

    protected abstract F copyFeature(F val);

    public AbstractNode(N o) {
        value = o.value;
        key = o.key;
        for (var entry : o.nodes.entrySet())
            nodes.put(entry.getKey(), entry.getValue().copy());
        Map<String, F> features = new HashMap<>();
        for (var entry : o.features.entrySet()) {
            F feature = copyFeature(entry.getValue());
            features.put(entry.getKey(), feature);
        }
        this.features = Collections.unmodifiableMap(features);
        treeData = o.treeData;
    }

    public abstract N self();

    @Override public C value() { return value; }

    @Override public @Nullable N parent() { return key == null ? null : key.parent; }
    @Override public @Nullable String key() { return key == null ? null : key.key; }

    @Override
    public NodePath path() {
        LinkedList<String> path = new LinkedList<>();
        Node current = this;
        do {
            path.addFirst(current.key());
        } while ((current = current.parent()) != null);
        return NodePath.path(path);
    }

    @Override
    public N parent(N parent, String key) {
        if (this.key != null)
            this.key.parent.removeNode(this.key.key);
        this.key = new NodeKey<>(parent, key);
        treeData = parent.treeData;
        return self();
    }

    @Override
    public N orphan() {
        if (key != null)
            key.parent.removeNode(key.key);
        key = null;
        treeData = null;
        return self();
    }

    @Override
    public N root() {
        return key == null ? self() : key.parent.root();
    }

    @Override
    public boolean isRoot() {
        return key == null;
    }

    @Override public Optional<TreeData.Scoped<N>> treeData() { return Optional.ofNullable(treeData); }

    @Override
    public Optional<N> node(String... path) {
        if (path.length == 0)
            return Optional.of(self());
        N next = nodes.get(path[0]);
        return next == null ? Optional.empty() : next.node(Arrays.copyOfRange(path, 1, path.length));
    }

    @Override
    public Optional<N> node(NodePath path) {
        return node(path.array());
    }

    @Override
    public Map<String, N> nodes() {
        return new HashMap<>(nodes);
    }

    @Override
    public N removeNode(String key) {
        N old = nodes.remove(key);
        if (old != null)
            old.orphan();
        return old;
    }

    @Override
    public N node(String key, N val) throws IncompatibilityException {
        Slot slot = value.slot(key)
                .orElseThrow(() -> new IllegalArgumentException("No slot '" + key + "' exists on component '" + value.id() + "'"));
        slot.compatibility(this, val);
        removeNode(key);
        nodes.put(key, val);
        val.parent(self(), key);
        return val;
    }

    public void unsafeNode(String key, N val) {
        nodes.put(key, val);
    }

    @Override
    public Optional<F> feature(String key) {
        return Optional.ofNullable(features.get(key));
    }

    @Override
    public Map<String, F> features() {
        return new HashMap<>(features);
    }

    @Override
    public N asRoot() {
        N result = copy();
        result.key = null;
        result.treeData = null;
        return result;
    }

    private record StatPair(Node node, List<StatIntermediate.MapData> data) {}

    @Override
    public <E extends NodeEvent<N>> E call(E event) {
        // Calling inherently rebuilds the tree
        key = null;
        treeData = BasicTreeData.blank();
        List<StatPair> forwardStats = new ArrayList<>();
        List<StatPair> reverseStats = new ArrayList<>();
        buildTree(event, forwardStats, reverseStats, this);

        for (List<StatPair> stats : Arrays.asList(forwardStats, reverseStats)) {
            for (var pair : stats) {
                pair.data.sort(Comparator.comparingInt(d -> d.priority().value()));
                for (var data : pair.data) {
                    try {
                        data.rule().applies(pair.node);
                        treeData.stats().chain(data.stats());
                    } catch (RuleException ignore) {}
                }
            }
        }

        treeData.events().call(event);
        return event;
    }

    protected void buildTree(NodeEvent<N> event, List<StatPair> forwardStats, List<StatPair> reverseStats, AbstractNode<N, C, F> parent) {
        StatIntermediate stats = new StatIntermediate(value.stats());
        for (var feature : features.values()) {
            feature.build(event, stats);
        }
        forwardStats.add(new StatPair(this, stats.forward()));
        reverseStats.add(0, new StatPair(this, stats.reverse()));
        for (var entry : nodes.entrySet()) {
            N node = entry.getValue();
            node.treeData = parent.treeData;
            node.key = new NodeKey<>(self(), entry.getKey());
            node.buildTree(event, forwardStats, reverseStats, this);
        }
    }

    @Override
    public String toString() {
        return value.id() + nodes;
    }
}
