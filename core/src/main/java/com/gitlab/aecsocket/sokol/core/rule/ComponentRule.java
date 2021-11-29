package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Renderable;
import com.gitlab.aecsocket.sokol.core.TreeContext;
import net.kyori.adventure.text.Component;

import java.util.*;

import static net.kyori.adventure.text.Component.*;
import static com.gitlab.aecsocket.sokol.core.rule.Rule.*;

public final class ComponentRule {
    private ComponentRule() {}

    private static Component formatStrings(Set<String> strings) {
        return text(String.join(",", strings), Renderable.CONSTANT);
    }

    public static final class MissingKeysException extends RuleException {
        private final Set<String> required;
        private final Set<String> found;

        private MissingKeysException(Rule rule, Set<String> required, Set<String> found) {
            super(rule, "Found " + found + ", required any of " + required);
            this.required = required;
            this.found = found;
        }

        public Set<String> required() { return required; }
        public Set<String> found() { return found; }
    }

    public static final class HasTags implements Rule {
        private final Set<String> tags;

        public HasTags(Set<String> tags) {
            this.tags = tags;
        }

        public Set<String> tags() { return tags; }

        @Override
        public void applies(Node node, TreeContext<?> treeCtx) throws RuleException {
            Set<String> nodeTags = node.value().tags();
            if (Collections.disjoint(nodeTags, tags))
                throw new MissingKeysException(this, tags, nodeTags);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String name() { return "has_tags"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HasTags hasTags = (HasTags) o;
            return tags.equals(hasTags.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tags);
        }

        @Override
        public String toString() { return "#" + tags; }

        @Override
        public Component render(Locale locale, Localizer lc) {
            return text("#", OPERATOR)
                    .append(wrapBrackets(formatStrings(tags)));
        }
    }

    public static final class HasFeatures implements Rule {
        private final Set<String> features;

        public HasFeatures(Set<String> features) {
            this.features = features;
        }

        public Set<String> features() { return features; }

        @Override
        public void applies(Node node, TreeContext<?> treeCtx) throws RuleException {
            Set<String> nodeFeatures = node.featureKeys();
            if (Collections.disjoint(nodeFeatures, features))
                throw new MissingKeysException(this, features, nodeFeatures);
        }

        @Override
        public String name() { return "has_features"; }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HasFeatures that = (HasFeatures) o;
            return features.equals(that.features);
        }

        @Override
        public int hashCode() {
            return Objects.hash(features);
        }

        @Override
        public String toString() { return "~" + features; }

        @Override
        public Component render(Locale locale, Localizer lc) {
            return text("~", OPERATOR)
                    .append(wrapBrackets(formatStrings(features)));
        }
    }

    public static final class IsComplete implements Rule {
        public static final IsComplete INSTANCE = new IsComplete();

        private IsComplete() {}

        @Override
        public void applies(Node node, TreeContext<?> treeCtx) throws RuleException {
            if (!treeCtx.complete())
                throw new Exception(this);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String name() { return "is_complete"; }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() { return "/~"; }

        @Override
        public Component render(Locale locale, Localizer lc) {
            return text("/~", OPERATOR);
        }

        public static final class Exception extends RuleException {
            private Exception(Rule rule) {
                super(rule);
            }
        }
    }
}
