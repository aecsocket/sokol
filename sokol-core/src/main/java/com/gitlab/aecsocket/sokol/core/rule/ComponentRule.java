package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Rules concerning components and tree nodes.
 */
public final class ComponentRule {
    private ComponentRule() {}

    /**
     * Gets if a tree is complete.
     */
    @ConfigSerializable
    public static final class Complete implements Rule {
        /** The rule type. */
        public static final String TYPE = "complete";
        /** A pre-made instance. */
        public static final Complete INSTANCE = new Complete();

        private Complete() {}

        @Override public @NotNull String type() { return TYPE; }

        @Override
        public boolean applies(@NotNull TreeNode node) {
            return node.complete();
        }

        @Override
        public void visit(@NotNull Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() { return getClass().hashCode(); }

        @Override
        public String toString() { return TYPE; }
    }

    /**
     * Gets if a tree node's component value has any of the specified tags.
     */
    @ConfigSerializable
    public static final class HasTags implements Rule {
        /** The rule type. */
        public static final String TYPE = "has_tags";

        @Required private final Set<String> tags;

        public HasTags(Set<String> tags) {
            this.tags = tags;
        }

        private HasTags() { this(null); }

        @Override public @NotNull String type() { return TYPE; }

        public Set<String> tags() { return tags; }

        @Override
        public boolean applies(@NotNull TreeNode node) {
            return !Collections.disjoint(node.value().tags(), tags);
        }

        @Override
        public void visit(@NotNull Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return "#" + tags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HasTags tagged = (HasTags) o;
            return tags.equals(tagged.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tags);
        }
    }

    /**
     * Gets if a tree node's component value has any of the specified systems.
     */
    @ConfigSerializable
    public static final class HasSystems implements Rule {
        /** The rule type. */
        public static final String TYPE = "has_systems";

        @Required private final Set<String> systems;

        public HasSystems(Set<String> systems) {
            this.systems = systems;
        }

        private HasSystems() { this(null); }

        @Override public @NotNull String type() { return TYPE; }

        public Set<String> systems() { return systems; }

        @Override
        public boolean applies(@NotNull TreeNode node) {
            return !Collections.disjoint(node.value().baseSystems().keySet(), systems);
        }

        @Override
        public void visit(@NotNull Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return TYPE + ":" + systems;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HasSystems hasSystems = (HasSystems) o;
            return systems.equals(hasSystems.systems);
        }

        @Override
        public int hashCode() {
            return Objects.hash(systems);
        }
    }
}
