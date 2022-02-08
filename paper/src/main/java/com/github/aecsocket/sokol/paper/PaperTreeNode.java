package com.github.aecsocket.sokol.paper;

import java.util.HashMap;
import java.util.Map;

import com.github.aecsocket.sokol.core.Tree;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.event.NodeEvent.CreateItem;
import com.github.aecsocket.sokol.core.impl.AbstractTreeNode;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class PaperTreeNode extends AbstractTreeNode<
    PaperTreeNode, PaperBlueprintNode, PaperComponent, PaperFeatureInstance, PaperItemStack
> {
    public PaperTreeNode(PaperTreeNode o) {
        super(o);
    }

    public PaperTreeNode(AbstractTreeNode<PaperTreeNode, PaperBlueprintNode, PaperComponent, PaperFeatureInstance, PaperItemStack> o) {
        super(o);
    }

    private PaperTreeNode(PaperComponent value, Map<String, PaperFeatureData> featureData, Context context, @Nullable Tree<PaperTreeNode> tree, @Nullable Key<PaperTreeNode> key) {
        super(value, featureData, context, tree, key);
    }

    public PaperTreeNode(PaperComponent value, Map<String, PaperFeatureData> featureData, Context context, @Nullable Tree<PaperTreeNode> tree, PaperTreeNode parent, String key) {
        super(value, featureData, context, tree, parent, key);
    }

    public PaperTreeNode(PaperComponent value, Map<String, PaperFeatureData> featureData, Context context, PaperTreeNode parent, String key) {
        super(value, featureData, context, parent, key);
    }

    public PaperTreeNode(PaperComponent value, Map<String, PaperFeatureData> featureData, Context context, @Nullable Tree<PaperTreeNode> tree) {
        super(value, featureData, context, tree);
    }

    public PaperTreeNode(PaperComponent value, Map<String, PaperFeatureData> featureData, Context context) {
        super(value, featureData, context);
    }

    @Override public PaperTreeNode self() { return this; }
    @Override public PaperTreeNode copy() { return new PaperTreeNode(this); }

    @Override public SokolPlugin platform() { return value.platform(); }

    private PaperBlueprintNode asBlueprintNode(@Nullable PaperBlueprintNode parent, @Nullable String key) {
        Map<String, PaperFeatureData> featureData = new HashMap<>();
        for (var entry : features.entrySet()) {
            featureData.put(entry.getKey(), entry.getValue().asData());
        }
        PaperBlueprintNode node = parent == null || key == null
            ? new PaperBlueprintNode(value, featureData)
            : new PaperBlueprintNode(value, featureData, parent, key);
        for (var entry : children.entrySet()) {
            String subKey = entry.getKey();
            node.setUnsafe(subKey, entry.getValue().asBlueprintNode(node, subKey));
        }
        return node;
    }

    @Override
    public PaperBlueprintNode asBlueprintNode() {
        return asBlueprintNode(null, null);
    }

    @Override
    protected PaperItemStack createItem() {
        return new PaperItemStack(value.platform(), tree.stats().require(PaperComponent.STAT_ITEM).stack());
    }

    @Override
    protected CreateItem<PaperTreeNode, PaperBlueprintNode, PaperItemStack> createItemEvent(PaperItemStack item) {
        return new PaperEvents.CreateItem(this, item);
    }
}
