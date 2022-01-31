package com.github.aecsocket.sokol.paper;

import java.util.Map;

import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.impl.AbstractBlueprintNode;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class PaperBlueprintNode extends AbstractBlueprintNode<
    PaperBlueprintNode, PaperTreeNode, PaperComponent, PaperFeatureData
> {
    private final SokolPlugin platform;

    public PaperBlueprintNode(PaperBlueprintNode o) {
        super(o);
        platform = o.platform;
    }

    public PaperBlueprintNode(SokolPlugin platform, AbstractBlueprintNode<PaperBlueprintNode, PaperTreeNode, PaperComponent, PaperFeatureData> o) {
        super(o);
        this.platform = platform;
    }

    private PaperBlueprintNode(SokolPlugin platform, PaperComponent value, Map<String, PaperFeatureData> featureData, @Nullable Key<PaperBlueprintNode> key) {
        super(value, featureData, key);
        this.platform = platform;
    }

    public PaperBlueprintNode(SokolPlugin platform, PaperComponent value, Map<String, PaperFeatureData> featureData, PaperBlueprintNode parent, String key) {
        super(value, featureData, parent, key);
        this.platform = platform;
    }

    public PaperBlueprintNode(SokolPlugin platform, PaperComponent value, Map<String, PaperFeatureData> featureData) {
        super(value, featureData);
        this.platform = platform;
    }

    @Override public PaperBlueprintNode self() { return this; }
    @Override public PaperBlueprintNode copy() { return new PaperBlueprintNode(this); }

    private PaperTreeNode asTreeNode(Context context, @Nullable PaperTreeNode parent, @Nullable String key) {
        PaperTreeNode node = parent == null || key == null
            ? new PaperTreeNode(platform, value, featureData, context)
            : new PaperTreeNode(platform, value, featureData, context, parent, key);
        for (var entry : children.entrySet()) {
            String subKey = entry.getKey();
            node.setUnsafe(subKey, entry.getValue().asTreeNode(context, node, subKey));
        }
        return node;
    }

    @Override
    public PaperTreeNode asTreeNode(Context context) {
        return asTreeNode(context, null, null).build();
    }
}
