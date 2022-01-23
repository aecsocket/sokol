package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.impl.AbstractComponent;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.paper.SokolPlugin;

import java.util.Map;
import java.util.Set;

public final class PaperComponent
        extends AbstractComponent<PaperComponent, PaperNodeSlot, PaperFeatureConfig> {
    private final SokolPlugin platform;

    public PaperComponent(SokolPlugin platform, String id, Set<String> tags, Map<String, PaperNodeSlot> slots, Map<String, PaperFeatureConfig> features, StatIntermediate stats) {
        super(id, tags, slots, features, stats);
        this.platform = platform;
    }

    public SokolPlugin platform() { return platform; }
}
