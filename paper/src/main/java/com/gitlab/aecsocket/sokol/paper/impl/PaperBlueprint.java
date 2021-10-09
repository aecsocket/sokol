package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.impl.AbstractBlueprint;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;

public class PaperBlueprint extends AbstractBlueprint<PaperNode> {
    private final SokolPlugin platform;

    public PaperBlueprint(SokolPlugin platform, String id, PaperNode node) {
        super(id, node);
        this.platform = platform;
    }

    @Override public SokolPlugin platform() { return platform; }
}
