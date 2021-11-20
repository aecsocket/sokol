package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.Quantifier;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public abstract class NodeHolderFeature<I extends NodeHolderFeature<I, N>.Instance, N extends Node.Scoped<N, ?, ?>> extends AbstractFeature<I, N> {
    public static final String ID = "node_holder";

    public enum Position {
        TOP,
        BOTTOM
    }

    protected final int listenerPriority;
    protected Rule rule;
    protected final Position headerPosition;
    protected final @Nullable Integer capacity;
    protected final boolean showFullAsDurability;

    public NodeHolderFeature(int listenerPriority, Rule rule, Position headerPosition, @Nullable Integer capacity, boolean showFullAsDurability) {
        this.listenerPriority = listenerPriority;
        this.rule = rule;
        this.headerPosition = headerPosition;
        this.capacity = capacity;
        this.showFullAsDurability = showFullAsDurability;
    }

    public int listenerPriority() { return listenerPriority; }
    public Rule rule() { return rule; }
    public Position headerPosition() { return headerPosition; }
    public Integer capacity() { return capacity; }
    public boolean showFullAsDurability() { return showFullAsDurability; }

    public abstract class Instance extends AbstractInstance<N> {
        protected final List<Quantifier<N>> nodes;

        public Instance(N parent, List<Quantifier<N>> nodes) {
            super(parent);
            this.nodes = nodes;
        }

        public Instance(N parent) {
            super(parent);
            nodes = new ArrayList<>();
        }

        @Override public NodeHolderFeature<I, N> type() { return NodeHolderFeature.this; }

        public List<Quantifier<N>> nodes() { return nodes; }

        protected abstract TypeToken<? extends CreateItemEvent<N>> eventCreateItem();

        @Override
        public void build(NodeEvent<N> event, StatIntermediate stats) {
            parent.treeData().ifPresent(treeData -> {
                var events = treeData.events();
                events.register(eventCreateItem(), this::onCreateItem, listenerPriority);
            });
        }

        private void onCreateItem(CreateItemEvent<N> event) {
            if (!parent.isRoot())
                return;
            Locale locale = event.locale();
            N node = event.node();
            List<Component> lines = new ArrayList<>();

            int total = Quantifier.total(nodes);
            List<Component> header = platform().lc().lines(locale, lcKey("header." + (total == 0 ? "empty"
                    : capacity != null && total >= capacity ? "full" : "default")),
                            "total", ""+total,
                            "capacity", ""+capacity)
                            .orElse(null);

            if (header != null && headerPosition == Position.TOP)
                lines.addAll(header);

            for (var qt : nodes) {
                platform().lc().lines(locale, lcKey("entry"),
                        "amount", ""+qt.amount(),
                        "value", qt.object().value().render(locale, platform().lc()))
                        .ifPresent(lines::addAll);
            }

            if (header != null && headerPosition == Position.BOTTOM)
                lines.addAll(header);

            event.item().addDescription(lines);

            if (capacity != null && showFullAsDurability)
                event.item().durability((double) total / capacity);
        }
    }

    @Override public String id() { return ID; }

    // todo
    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) {
        return Optional.empty();
    }
}
