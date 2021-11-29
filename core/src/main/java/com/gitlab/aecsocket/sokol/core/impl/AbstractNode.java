package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.rule.RuleException;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public abstract class AbstractNode<
        N extends AbstractNode<N, I, C, F>,
        I extends Item.Scoped<I, N>,
        C extends Component.Scoped<C, ?, ? extends Feature<? extends F, N>, N>,
        F extends FeatureInstance<N>
> implements Node.Scoped<N, I, C, F> {
    public static final String TAG_REQUIRED = "required";

    public static boolean required(Slot slot) {
        return slot.tagged(TAG_REQUIRED);
    }

    protected record NodeKey<N extends Node>(N parent, String key) {}

    protected final C value;
    protected @Nullable NodeKey<N> key;
    protected final Map<String, N> nodes = new HashMap<>();
    protected final Map<String, F> features;

    protected AbstractNode(C value, @Nullable NodeKey<N> key, Map<String, F> features) {
        this.value = value;
        this.key = key;
        this.features = features;
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
        return self();
    }

    @Override
    public N orphan() {
        if (key != null)
            key.parent.removeNode(key.key);
        key = null;
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
    public Set<String> nodeKeys() {
        return nodes.keySet();
    }

    @Override
    public Collection<N> nodeValues() {
        return nodes.values();
    }

    @Override
    public N removeNode(String key) {
        N old = nodes.remove(key);
        if (old != null)
            old.orphan();
        return old;
    }

    @Override
    public N node(String key, N val, TreeContext<N> ctx, TreeContext<N> childCtx) throws IncompatibilityException {
        Slot slot = value.slot(key)
                .orElseThrow(() -> new IllegalArgumentException("No slot '" + key + "' exists on component '" + value.id() + "'"));
        slot.compatibility(self(), val, ctx, childCtx);
        removeNode(key);
        nodes.put(key, val);
        val.parent(self(), key);
        return val;
    }

    public void forceNode(String key, @Nullable N val) {
        if (val == null)
            nodes.remove(key);
        else
            nodes.put(key, val);
    }

    @Override
    public Map<String, F> features() {
        return new HashMap<>(features);
    }

    @Override
    public Set<String> featureKeys() {
        return features.keySet();
    }

    @Override
    public Collection<F> featureValues() {
        return features.values();
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

    protected void build(TreeContext<N> ctx, List<StatPair> forwardStats, List<StatPair> reverseStats, N parent) {
        StatIntermediate stats = new StatIntermediate(value.stats());
        for (var feature : features.values()) {
            feature.build(ctx, stats);
        }
        forwardStats.add(new StatPair(this, stats.forward()));
        reverseStats.add(0, new StatPair(this, stats.reverse()));
        for (var entry : value.slots().entrySet()) {
            String key = entry.getKey();
            if (required(entry.getValue()) && !nodes.containsKey(key)) {
                ctx.addIncomplete(path().append(key));
            }
        }
        for (var entry : nodes.entrySet()) {
            N node = entry.getValue();
            node.key = new NodeKey<>(self(), entry.getKey());
            node.build(ctx, forwardStats, reverseStats, self());
        }
    }

    private record StatPair(Node node, List<StatIntermediate.MapData> data) {}

    protected <T extends TreeContext<N>> T build(T ctx) {
        List<StatPair> forwardStats = new ArrayList<>();
        List<StatPair> reverseStats = new ArrayList<>();
        build(ctx, forwardStats, reverseStats, self());

        for (List<StatPair> stats : Arrays.asList(forwardStats, reverseStats)) {
            for (var pair : stats) {
                pair.data.sort(Comparator.comparingInt(d -> d.priority().value()));
                for (var data : pair.data) {
                    try {
                        data.rule().applies(pair.node, ctx);
                        ctx.stats().chain(data.stats());
                    } catch (RuleException ignore) {}
                }
            }
        }

        return ctx;
    }

    @Override
    public TreeContext<N> build(Locale locale) {
        return build(new BasicTreeContext<>(locale));
    }

    @Override
    public TreeContext<N> build(ItemUser user) {
        return build(new BasicTreeContext<>(user));
    }

    @Override
    public N asRoot() {
        N result = copy();
        result.key = null;
        return result;
    }

    @Override
    public String toString() {
        return value.id() + nodes;
    }
}
