package com.github.aecsocket.sokol.paper;

import java.util.Map;
import java.util.Set;

import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.impl.AbstractComponent;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;

public final class PaperComponent extends AbstractComponent<
    PaperComponent, PaperNodeSlot, PaperFeatureProfile
> {
    private final SokolPlugin platform;

    PaperComponent(SokolPlugin platform, String id, Set<String> tags, Map<String, PaperFeatureProfile> features, Map<String, PaperNodeSlot> slots, StatIntermediate stats) {
        super(id, tags, features, slots, stats);
        this.platform = platform;
    }

    @Override public SokolPlatform platform() { return platform; }
}
