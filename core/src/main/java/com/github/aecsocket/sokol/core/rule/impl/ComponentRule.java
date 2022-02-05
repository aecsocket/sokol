package com.github.aecsocket.sokol.core.rule.impl;

import com.github.aecsocket.sokol.core.SokolNode;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.rule.RuleException;

import java.util.Collections;
import java.util.Set;

public final class ComponentRule {
    private ComponentRule() {}

    public static final class HasTags implements Rule {
        private final Set<String> tags;

        public HasTags(Set<String> tags) {
            this.tags = tags;
        }

        public Set<String> tags() { return tags; }

        @Override
        public void applies(SokolNode target) throws RuleException {
            if (Collections.disjoint(tags, target.value().tags()))
                throw new RuleException();
        }
    }

    public static final class HasFeatures implements Rule {
        private final Set<String> features;

        public HasFeatures(Set<String> features) {
            this.features = features;
        }

        public Set<String> tags() { return features; }

        @Override
        public void applies(SokolNode target) throws RuleException {
            if (Collections.disjoint(features, target.value().features().keySet()))
                throw new RuleException();
        }
    }

    public static final class IsComplete implements Rule {
        public static final IsComplete INSTANCE = new IsComplete();

        private IsComplete() {}

        @Override
        public void applies(SokolNode target) throws RuleException {
            if (!target.complete())
                throw new RuleException();
        }
    }
}
