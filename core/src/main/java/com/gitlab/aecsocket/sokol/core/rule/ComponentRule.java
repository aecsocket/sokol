package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.node.RuleException;

import java.util.Collections;
import java.util.Objects;
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
        public void applies(Node node) throws RuleException {
            if (Collections.disjoint(node.value().tags(), tags))
                throw new RuleException(this, "Node is not tagged");
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
    }

    public static final class HasFeatures implements Rule {
        private final Set<String> features;

        public HasFeatures(Set<String> features) {
            this.features = features;
        }

        public Set<String> features() { return features; }

        @Override
        public void applies(Node node) throws RuleException {
            if (Collections.disjoint(node.value().featureTypes().keySet(), features))
                throw new RuleException(this, "Node does not have features");
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
    }
}
