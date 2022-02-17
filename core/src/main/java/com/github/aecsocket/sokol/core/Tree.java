package com.github.aecsocket.sokol.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.github.aecsocket.minecommons.core.event.EventDispatcher;
import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.rule.RuleException;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatMap;

public record Tree<N extends TreeNode.Scoped<N, ?, ?, ?, ?>>(
    N root,
    EventDispatcher<NodeEvent<N>> events,
    StatMap stats,
    List<NodePath> incomplete
) {
    private record StatPair<N extends TreeNode.Scoped<N, ?, ?, ?, ?>>(N node, List<StatIntermediate.MapData> data) {}

    private static <N extends TreeNode.Scoped<N, ?, ?, ?, ?>> void build(
        Tree<N> tree, List<StatPair<N>> forward, List<StatPair<N>> reverse, N node, String... path
    ) {
        node.tree(tree);
        StatIntermediate stats = new StatIntermediate(node.value().stats());
        for (var feature : node.features().values()) {
            feature.build(tree, node, stats);
        }
        forward.add(new StatPair<>(node, stats.forward()));
        reverse.add(0, new StatPair<>(node, stats.reverse()));

        for (var entry : node.value().slots().entrySet()) {
            String key = entry.getKey();
            String[] newPath = Arrays.copyOfRange(path, 0, path.length + 1);

            node.get(key).ifPresentOrElse(
                child -> build(tree, forward, reverse, child, newPath),
                () -> {
                    if (entry.getValue().required()) {
                        newPath[path.length] = key;
                        tree.incomplete(NodePath.path(newPath));
                    }
                }
            );
        }
    }

    public static <N extends TreeNode.Scoped<N, ?, ?, ?, ?>> Tree<N> build(N root) {
        Tree<N> tree = new Tree<>(root, new EventDispatcher<>(), new StatMap(), new ArrayList<>());
        List<StatPair<N>> forward = new ArrayList<>();
        List<StatPair<N>> reverse = new ArrayList<>();

        build(tree, forward, reverse, root);

        for (var pairs : Arrays.asList(forward, reverse)) {
            for (var pair : pairs) {
                pair.data.sort(Comparator.comparingInt(data -> data.priority().value()));
                for (var data : pair.data) {
                    try {
                        data.rule().applies(pair.node);
                        tree.stats.chain(data.entries());
                    } catch (RuleException ignore) {}
                }
            }
        }

        return tree;
    }

    public void incomplete(NodePath path) {
        incomplete.add(path);
    }

    public boolean complete() {
        return incomplete.isEmpty();
    }

    public <E extends NodeEvent<N>> E call(E event) {
        return events.call(event);
    }

    public <E extends NodeEvent<N>> E andCall(Function<N, E> event) {
        return events.call(event.apply(root));
    }
}
