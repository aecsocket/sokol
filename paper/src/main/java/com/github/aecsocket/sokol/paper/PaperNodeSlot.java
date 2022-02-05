package com.github.aecsocket.sokol.paper;

import java.util.Set;

import com.github.aecsocket.sokol.core.impl.BasicNodeSlot;
import com.github.aecsocket.sokol.core.rule.BlueprintRule;

public final class PaperNodeSlot extends BasicNodeSlot<
    PaperNodeSlot, PaperComponent
> {
    PaperNodeSlot(PaperComponent parent, String key, Set<String> tags, BlueprintRule rule) {
        super(parent, key, tags, rule);
    }
}
