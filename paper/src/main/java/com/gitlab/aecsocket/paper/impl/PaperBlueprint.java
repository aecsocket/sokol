package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.impl.AbstractBlueprint;
import com.gitlab.aecsocket.paper.SokolPlugin;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

public final class PaperBlueprint
        extends AbstractBlueprint<PaperBlueprint, PaperNode, PaperComponent, PaperFeatureData> {
    private final SokolPlugin platform;

    public PaperBlueprint(PaperBlueprint o) {
        super(o);
        platform = o.platform;
    }

    public PaperBlueprint(SokolPlugin platform, AbstractBlueprint<PaperBlueprint, PaperNode, PaperComponent, PaperFeatureData> o) {
        super(o);
        this.platform = platform;
    }

    public PaperBlueprint(SokolPlugin platform, PaperComponent value, Map<String, PaperFeatureData> featureData, @Nullable Key<PaperBlueprint> key) {
        super(value, featureData, key);
        this.platform = platform;
    }

    public PaperBlueprint(SokolPlugin platform, PaperComponent value, Map<String, PaperFeatureData> featureData, PaperBlueprint parent, String key) {
        super(value, featureData, parent, key);
        this.platform = platform;
    }

    public PaperBlueprint(SokolPlugin platform, PaperComponent value, Map<String, PaperFeatureData> featureData) {
        super(value, featureData);
        this.platform = platform;
    }

    @Override public PaperBlueprint self() { return this; }
    @Override public PaperBlueprint copy() { return new PaperBlueprint(this); }
    @Override public SokolPlugin platform() { return platform; }

    private PaperNode asNode(Context context, @Nullable PaperNode parent, @Nullable String key) {
        PaperNode node = parent == null || key == null
                ? new PaperNode(platform, value, context, features)
                : new PaperNode(platform, value, context, features, parent, key);
        for (var entry : children.entrySet()) {
            String subKey = entry.getKey();
            node.setUnsafe(subKey, entry.getValue().asNode(context, node, subKey));
        }
        return node;
    }

    @Override
    public PaperNode asNode(Context context) {
        PaperNode node = asNode(context, null, null);
        node.build();
        return node;
    }
}
