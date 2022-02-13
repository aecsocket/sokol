package com.github.aecsocket.sokol.paper;

import java.util.Map;

import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.impl.AbstractBlueprintNode;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class PaperBlueprintNode extends AbstractBlueprintNode<
    PaperBlueprintNode, PaperTreeNode, PaperComponent, PaperFeatureData<?, ?>
> implements PaperNode {
    public PaperBlueprintNode(PaperBlueprintNode o) {
        super(o);
    }

    public PaperBlueprintNode(AbstractBlueprintNode<PaperBlueprintNode, PaperTreeNode, PaperComponent, PaperFeatureData<?, ?>> o) {
        super(o);
    }

    private PaperBlueprintNode(PaperComponent value, Map<String, PaperFeatureData<?, ?>> featureData, @Nullable Key<PaperBlueprintNode> key) {
        super(value, featureData, key);
    }

    public PaperBlueprintNode(PaperComponent value, Map<String, PaperFeatureData<?, ?>> featureData, PaperBlueprintNode parent, String key) {
        super(value, featureData, parent, key);
    }

    public PaperBlueprintNode(PaperComponent value, Map<String, PaperFeatureData<?, ?>> featureData) {
        super(value, featureData);
    }

    public PaperBlueprintNode(PaperComponent value) {
        super(value);
    }

    @Override public PaperBlueprintNode self() { return this; }
    @Override public PaperBlueprintNode copy() { return new PaperBlueprintNode(this); }

    private PaperTreeNode asTreeNode(Context context, @Nullable PaperTreeNode parent, @Nullable String key) {
        PaperTreeNode node = parent == null || key == null
            ? new PaperTreeNode(value, featureData, context)
            : new PaperTreeNode(value, featureData, context, parent, key);
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

    public static final class Serializer extends AbstractBlueprintNode.Serializer<
        PaperBlueprintNode, PaperTreeNode, PaperComponent, PaperFeature<?>, PaperFeatureProfile<?, ?>, PaperFeatureData<?, ?>, PaperFeatureInstance<?>
    > {
        private final SokolPlugin platform;

        public Serializer(SokolPlugin platform) {
            this.platform = platform;
        }

        @Override public SokolPlugin platform() { return platform; }

        @Override
        protected PaperBlueprintNode create(
            PaperComponent value, Map<String, PaperFeatureData<?, ?>> featureData, @Nullable PaperBlueprintNode parent, @Nullable String key
        ) {
            if (parent == null || key == null)
                return new PaperBlueprintNode(value, featureData);
            else
                return new PaperBlueprintNode(value, featureData, parent, key);
        }
    }
}
