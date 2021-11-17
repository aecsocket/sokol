package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.event.LocalizedEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.event.UserEvent;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.node.RuleException;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public abstract class AbstractNode<
        N extends AbstractNode<N, C, F>,
        C extends Component.Scoped<C, ?, ? extends Feature<? extends F, N>, N>,
        F extends FeatureInstance<N>
> implements Node.Scoped<N, C, F> {
    public static final String TAG_REQUIRED = "required";

    protected record NodeKey<N extends Node>(N parent, String key) {}

    protected final C value;
    protected @Nullable NodeKey<N> key;
    protected TreeData.Scoped<N> treeData;
    protected final Map<String, N> nodes = new HashMap<>();
    protected final Map<String, F> features;

    protected AbstractNode(C value, @Nullable NodeKey<N> key, Map<String, F> features, TreeData.@Nullable Scoped<N> treeData) {
        this.value = value;
        this.key = key;
        this.features = features;
        this.treeData = treeData;
    }

    protected AbstractNode(C value, @Nullable NodeKey<N> key) {
        this.value = value;
        this.key = key;
        features = new HashMap<>();
        for (var entry : value.features().entrySet()) {
            features.put(entry.getKey(), entry.getValue().create(self()));
        }
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
        features = new HashMap<>();
        for (var entry : o.features.entrySet()) {
            F feature = copyFeature(entry.getValue());
            features.put(entry.getKey(), feature);
        }
        treeData = o.treeData;
    }

    public abstract N self();

    @Override public C value() { return value; }

    @Override public @Nullable N parent() { return key == null ? null : key.parent; }
    @Override public @Nullable String key() { return key == null ? null : key.key; }

    @Override
    public NodePath path() {
        LinkedList<String> path = new LinkedList<>();
        for (var cur = this; cur != null && cur.key != null; cur = cur.parent())
            path.addFirst(cur.key());
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
    public Map<String, F> features() {
        return new HashMap<>(features);
    }

    @Override
    public Optional<F> feature(String key) {
        return Optional.ofNullable(features.get(key));
    }

    @Override
    public N feature(String key, F feature) {
        if (!features.containsKey(key))
            throw new IllegalArgumentException("No feature '" + key + "' exists on component '" + value.id() + "'");
        F old = features.get(key);
        if (!old.getClass().isInstance(feature))
            throw new IllegalArgumentException("Feature '" + feature.type().id() + "' of class " + feature.getClass().getName() +
                    " is not an instance of " + old.getClass().getName());
        features.put(key, feature);
        return self();
    }

    public void fillDefaultFeatures() {
        for (var entry : value.features().entrySet()) {
            var key = entry.getKey();
            if (!features.containsKey(key)) {
                features.put(key, entry.getValue().create(self()));
            }
        }
    }

    @Override
    public N initialize() {
        call(new Events.Initialize<>(self()));
        return self();
    }

    @Override
    public N initialize(Locale locale) {
        call(new Events.InitializeLocalized<>(self(), locale));
        return self();
    }

    @Override
    public N initialize(ItemUser user) {
        call(new Events.InitializeUser<>(self(), user));
        return self();
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
        for (var entry : value.slots().entrySet()) {
            String key = entry.getKey();
            if (entry.getValue().tagged(TAG_REQUIRED) && !nodes.containsKey(key)) {
                treeData.addIncomplete(path().append(key));
            }
        }
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

    public static final class Events {
        private Events() {}

        public record Initialize<N extends Node.Scoped<N, ?, ?>>(
                N node
        ) implements NodeEvent<N> {}

        public record InitializeLocalized<N extends Node.Scoped<N, ?, ?>>(
                N node,
                Locale locale
        ) implements LocalizedEvent<N> {}

        public record InitializeUser<N extends Node.Scoped<N, ?, ?>>(
                N node,
                ItemUser user
        ) implements UserEvent<N> {}
    }
}
