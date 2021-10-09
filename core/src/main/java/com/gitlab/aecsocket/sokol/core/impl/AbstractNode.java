package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.node.RuleException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
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
    protected final Map<String, N> nodes = new HashMap<>();
    protected final Map<String, ? extends F> features;

    protected EventDispatcher<NodeEvent<N>> events;
    protected StatMap stats;

    private AbstractNode(C value, @Nullable NodeKey<N> key) {
        this.value = value;
        this.key = key;
        Map<String, F> features = new HashMap<>();
        for (var entry : value.featureTypes().entrySet()) {
            F feature = entry.getValue().create(self());
            features.put(entry.getKey(), feature);
        }
        this.features = Collections.unmodifiableMap(features);
        events = new EventDispatcher<>();
        stats = new StatMap();
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
        events = o.events;
        stats = o.stats;
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
        events = parent.events;
        stats = parent.stats;
        return self();
    }

    @Override
    public N orphan() {
        if (key != null)
            key.parent.removeNode(key.key);
        makeRoot();
        return self();
    }

    public N makeRoot() {
        key = null;
        events = new EventDispatcher<>();
        stats = new StatMap();
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
    public N node(String key, N val) throws RuleException {
        Slot slot = value.slot(key)
                .orElseThrow(() -> new IllegalArgumentException("No slot [" + key + "] exists on component [" + value.id() + "]"));
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
    public EventDispatcher<NodeEvent<N>> events() { return events; }

    @Override
    public StatMap stats() { return stats; }

    @Override
    public N asRoot() {
        return copy().makeRoot();
    }

    @Override
    public void build(NodeEvent<N> event) {
        makeRoot();
        StatIntermediate stats = new StatIntermediate();
        build(event, stats, self());
    }

    protected void build(NodeEvent<N> event, StatIntermediate stats, N parent) {
        for (var feature : features.values()) {
            feature.build(event, stats);
        }
        for (var node : nodes.values())
            node.build(event, stats, self());
    }
}
