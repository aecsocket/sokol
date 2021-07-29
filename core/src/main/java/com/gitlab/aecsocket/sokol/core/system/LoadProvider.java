package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;

import java.util.Collections;
import java.util.Map;

public interface LoadProvider {
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

    static LoadProvider empty() {
        return new LoadProvider() {
            @Override public Map<String, Stat<?>> statTypes() { return Collections.emptyMap(); }
            @Override public Map<String, Class<? extends Rule>> ruleTypes() { return Collections.emptyMap(); }
        };
    }

    static LoadProvider ofStats(Map<String, Stat<?>> statTypes) {
        return new LoadProvider() {
            @Override public Map<String, Stat<?>> statTypes() { return statTypes; }
            @Override public Map<String, Class<? extends Rule>> ruleTypes() { return Collections.emptyMap(); }
        };
    }

    static LoadProvider ofRules(Map<String, Class<? extends Rule>> ruleTypes) {
        return new LoadProvider() {
            @Override public Map<String, Stat<?>> statTypes() { return Collections.emptyMap(); }
            @Override public Map<String, Class<? extends Rule>> ruleTypes() { return ruleTypes; }
        };
    }

    static LoadProvider ofBoth(Map<String, Stat<?>> statTypes, Map<String, Class<? extends Rule>> ruleTypes) {
        return new LoadProvider() {
            @Override public Map<String, Stat<?>> statTypes() { return statTypes; }
            @Override public Map<String, Class<? extends Rule>> ruleTypes() { return ruleTypes; }
        };
    }
}
