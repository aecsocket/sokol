package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;

import java.util.Collections;
import java.util.Map;

public interface LoadProvider extends Keyed {
    /**
     * Gets the stat types that this system defines, used for deserialization.
     * @return The stat types.
     */
    default Map<String, Stat<?>> statTypes() { return Collections.emptyMap(); }

    /**
     * Gets the rule types that this system defines, used for deserialization.
     * @return The rule types.
     */
    default Map<String, Class<? extends Rule>> ruleTypes() { return Collections.emptyMap(); }

    static LoadProvider empty(String id) {
        return new LoadProviderImpl(id, Collections.emptyMap(), Collections.emptyMap());
    }

    static LoadProvider ofStats(String id, Map<String, Stat<?>> statTypes) {
        return new LoadProviderImpl(id, statTypes, Collections.emptyMap());
    }

    static LoadProvider ofRules(String id, Map<String, Class<? extends Rule>> ruleTypes) {
        return new LoadProviderImpl(id, Collections.emptyMap(), ruleTypes);
    }

    static LoadProvider ofBoth(String id, Map<String, Stat<?>> statTypes, Map<String, Class<? extends Rule>> ruleTypes) {
        return new LoadProviderImpl(id, statTypes, ruleTypes);
    }
}
