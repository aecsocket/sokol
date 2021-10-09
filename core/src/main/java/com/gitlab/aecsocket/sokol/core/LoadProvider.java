package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;

import java.util.Map;

public interface LoadProvider extends Keyed {
    StatTypes statTypes();
    Map<String, Class<? extends Rule>> ruleTypes();
}
