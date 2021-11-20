package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Renderable;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static net.kyori.adventure.text.Component.*;
import static com.gitlab.aecsocket.sokol.core.rule.Rule.*;

public final class ComponentRule {
    private ComponentRule() {}

    private static Component formatStrings(Set<String> strings) {
        return text(String.join(",", strings), Renderable.CONSTANT);
    }

    public static final class HasTags implements Rule {
        private final Set<String> tags;

        public HasTags(Set<String> tags) {
            this.tags = tags;
        }

        public Set<String> tags() { return tags; }

        @Override
        public void applies(Node node) throws RuleException {
            if (Collections.disjoint(node.value().tags(), tags))
                throw new RuleException(this, "not_have_tag",
                        "has", String.join(", ", node.value().tags()),
                        "requires", String.join(", ", tags));
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

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
        public void applies(Node node) throws RuleException {
            if (Collections.disjoint(node.value().features().keySet(), features))
                throw new RuleException(this, "not_have_features",
                        "has", String.join(", ", node.value().features().keySet()),
                        "requires", String.join(", ", features));
        }

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
        public void applies(Node node) throws RuleException {
            if (!node.treeData().orElseThrow(() -> new RuleException(this, "no_tree_data")).complete())
                throw new RuleException(this, "not_complete");
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

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
    }
}
