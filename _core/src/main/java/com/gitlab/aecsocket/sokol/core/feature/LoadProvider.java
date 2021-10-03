package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;

import java.util.Collections;
import java.util.Map;

public interface LoadProvider extends Keyed {
    /**
     * Gets the stat types that this system defines, used for deserialization.i
     * @return The stat types.
     */
    default StatTypes statTypes() { return StatTypes.empty(); }

    /**
     * Gets the rule types that this system defines, used for deserialization.
     * @return The rule types.
     */
    default Map<String, Class<? extends Rule>> ruleTypes() { return Collections.emptyMap(); }

    static LoadProvider empty(String id) {
        return new LoadProviderImpl(id, StatTypes.empty(), Collections.emptyMap());
    }

    static LoadProvider ofStats(String id, StatTypes statTypes) {
        return new LoadProviderImpl(id, statTypes, Collections.emptyMap());
    }

    static LoadProvider ofRules(String id, Map<String, Class<? extends Rule>> ruleTypes) {
        return new LoadProviderImpl(id, StatTypes.empty(), ruleTypes);
    }

    static LoadProvider ofBoth(String id, StatTypes statTypes, Map<String, Class<? extends Rule>> ruleTypes) {
        return new LoadProviderImpl(id, statTypes, ruleTypes);
    }
}
