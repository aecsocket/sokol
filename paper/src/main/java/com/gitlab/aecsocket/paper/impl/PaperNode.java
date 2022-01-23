package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.api.Tree;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.event.CreateItemEvent;
import com.github.aecsocket.sokol.core.impl.AbstractNode;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.paper.SokolPlugin;
import com.gitlab.aecsocket.paper.stat.ItemStat;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.gitlab.aecsocket.paper.stat.ItemStat.*;

public final class PaperNode
        extends AbstractNode<PaperNode, PaperBlueprint, PaperComponent, PaperFeatureInstance, PaperItemStack> {
    public static final ItemStat STAT_ITEM = itemStat("item");
    public static final StatTypes STAT_TYPES = StatTypes.builder()
            .add(STAT_ITEM)
            .build();

    private final SokolPlugin platform;

    public PaperNode(PaperNode o) {
        super(o);
        platform = o.platform;
    }

    public PaperNode(SokolPlugin platform, AbstractNode<PaperNode, PaperBlueprint, PaperComponent, PaperFeatureInstance, PaperItemStack> o) {
        super(o);
        this.platform = platform;
    }

    private PaperNode(SokolPlugin platform, PaperComponent value, Context context, Map<String, PaperFeatureData> featureData, @Nullable Key<PaperNode> key, @Nullable Tree<PaperNode> tree) {
        super(value, context, featureData, key, tree);
        this.platform = platform;
    }

    public PaperNode(SokolPlugin platform, PaperComponent value, Context context, Map<String, PaperFeatureData> featureData, PaperNode parent, String key, Tree<PaperNode> tree) {
        super(value, context, featureData, parent, key, tree);
        this.platform = platform;
    }

    public PaperNode(SokolPlugin platform, PaperComponent value, Context context, Map<String, PaperFeatureData> featureData, PaperNode parent, String key) {
        super(value, context, featureData, parent, key);
        this.platform = platform;
    }

    public PaperNode(SokolPlugin platform, PaperComponent value, Context context, Map<String, PaperFeatureData> featureData, Tree<PaperNode> tree) {
        super(value, context, featureData, tree);
        this.platform = platform;
    }

    public PaperNode(SokolPlugin platform, PaperComponent value, Context context, Map<String, PaperFeatureData> featureData) {
        super(value, context, featureData);
        this.platform = platform;
    }

    @Override public PaperNode self() { return this; }
    @Override public PaperNode copy() { return new PaperNode(this); }
    @Override public SokolPlugin platform() { return platform; }

    private PaperBlueprint asBlueprint(@Nullable PaperBlueprint parent, @Nullable String key) {
        Map<String, PaperFeatureData> featureData = new HashMap<>();
        for (var entry : features.entrySet()) {
            featureData.put(entry.getKey(), entry.getValue().save());
        }
        PaperBlueprint blueprint = parent == null || key == null
                ? new PaperBlueprint(platform, value, featureData)
                : new PaperBlueprint(platform, value, featureData, parent, key);
        for (var entry : children.entrySet()) {
            String subKey = entry.getKey();
            blueprint.setUnsafe(subKey, entry.getValue().asBlueprint(blueprint, subKey));
        }
        return blueprint;
    }

    @Override
    public PaperBlueprint asBlueprint() {
        return asBlueprint(null, null);
    }

    @Override
    protected PaperItemStack createItem() {
        return new PaperItemStack(platform, tree.stats().require(STAT_ITEM).buildStack());
    }

    @Override
    protected CreateItemEvent<PaperNode, PaperBlueprint, PaperItemStack> createItemEvent(PaperItemStack item) {
        return new Events.CreateItem(this, item);
    }

    public static final class Events {
        private Events() {}

        public record CreateItem(
                PaperNode node,
                PaperItemStack item
        ) implements CreateItemEvent<PaperNode, PaperBlueprint, PaperItemStack> {}
    }
}
