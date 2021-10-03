package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;

import java.util.Map;

public record LoadProviderImpl(
        String id,
        StatTypes statTypes,
        Map<String, Class<? extends Rule>> ruleTypes
) implements LoadProvider {}
