package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.impl.BasicSlot;
import com.gitlab.aecsocket.sokol.core.Component;
import com.gitlab.aecsocket.sokol.core.rule.Rule;

import java.util.Set;

public class PaperSlot extends BasicSlot {
    public PaperSlot(Component parent, String key, Set<String> tags, Rule rule) {
        super(parent, key, tags, rule);
    }
}
