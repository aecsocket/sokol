package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.impl.AbstractComponent;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;

import java.util.Map;
import java.util.Set;

public class PaperComponent extends AbstractComponent<PaperComponent, PaperSlot, PaperFeature<?>, PaperNode> {
    private final SokolPlugin platform;

    public PaperComponent(String id, Set<String> tags, Map<String, PaperSlot> slots, Map<String, PaperFeature<?>> featureTypes, SokolPlugin platform) {
        super(id, tags, slots, featureTypes);
        this.platform = platform;
    }

    @Override
    public SokolPlugin platform() { return platform; }
}
