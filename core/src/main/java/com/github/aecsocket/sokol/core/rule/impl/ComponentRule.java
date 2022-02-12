package com.github.aecsocket.sokol.core.rule.impl;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.sokol.core.SokolNode;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.rule.RuleException;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import static net.kyori.adventure.text.Component.*;

public final class ComponentRule {
    private ComponentRule() {}

    public static final class HasTags implements Rule {
        public static final String RULE_HAS_TAGS = "rule.has_tags";

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

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_HAS_TAGS,
                c -> c.of("tags", () -> text(String.join(", ", tags))));
        }
    }

    public static final class HasFeatures implements Rule {
        public static final String RULE_HAS_FEATURES = "rule.has_features";

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

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_HAS_FEATURES,
                c -> c.of("features", () -> text(String.join(", ", features))));
        }
    }

    public static final class IsComplete implements Rule {
        public static final IsComplete INSTANCE = new IsComplete();
        public static final String RULE_IS_COMPLETE = "rule.is_complete";

        private IsComplete() {}

        @Override
        public void applies(SokolNode target) throws RuleException {
            if (!target.complete())
                throw new RuleException();
        }

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_IS_COMPLETE);
        }
    }
}
