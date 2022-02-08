package com.github.aecsocket.sokol.paper;

import java.util.Set;

import com.github.aecsocket.sokol.core.impl.BasicNodeSlot;
import com.github.aecsocket.sokol.core.rule.Rule;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public final class PaperNodeSlot extends BasicNodeSlot<
    PaperNodeSlot, PaperComponent
> {
    PaperNodeSlot(PaperComponent parent, String key, Set<String> tags, Rule rule) {
        super(parent, key, tags, rule);
    }

    private PaperNodeSlot() {
        super();
    }
}
