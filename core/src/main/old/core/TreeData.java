package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.context.Context;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;

import java.util.ArrayList;
import java.util.List;

public record TreeData<N extends Node.Scoped<N, ?, ?, ?, ?>>(
        Context context,
        EventDispatcher<NodeEvent<N>> events,
        StatMap stats
) {
    public <E extends NodeEvent<N>> E call(E event) {
        return events.call(event);
    }

    private record StatPair(Node node, List<StatIntermediate.MapData> data) {}



    private void build(N node) {

        StatIntermediate stats = new StatIntermediate(node.value().stats());
        for (var feature : node.featureValues()) {
            feature.build();
        }
    }
    
    public void build(N root) {
        List<StatPair> forward = new ArrayList<>();
        List<StatPair> reverse = new ArrayList<>();
        build();
    }

    /*protected void build(Tree<N> ctx, List<StatPair> forwardStats, List<StatPair> reverseStats, N parent) {
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

    protected <T extends Tree<N>> T build(T ctx) {
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
    }*/
}
