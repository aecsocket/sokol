package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;

import java.util.Map;

public record LoadProviderImpl(
        String id,
        Map<String, Stat<?>> statTypes,
        Map<String, Class<? extends Rule>> ruleTypes
) implements LoadProvider {}
