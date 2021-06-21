package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.Objects;

public final class ComponentRule {
    private ComponentRule() {}

    @ConfigSerializable
    public static final class Complete implements Rule {
        public static final String TYPE = "complete";
        public static final Complete INSTANCE = new Complete();

        private Complete() {}

        @Override public String type() { return TYPE; }

        @Override
        public boolean applies(TreeNode tree) {
            return tree.complete();
        }

        @Override
        public void visit(Visitor visitor) {
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

    @ConfigSerializable
    public static final class HasTag implements Rule {
        public static final String TYPE = "has_tag";

        @Required private final String tag;

        public HasTag(String tag) {
            this.tag = tag;
        }

        private HasTag() { this(null); }

        @Override public String type() { return TYPE; }

        public String tag() { return tag; }

        @Override
        public boolean applies(TreeNode tree) {
            return tree.value().tagged(tag);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return TYPE + ":" + tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HasTag tagged = (HasTag) o;
            return tag.equals(tagged.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tag);
        }
    }

    @ConfigSerializable
    public static final class HasSystem implements Rule {
        public static final String TYPE = "has_system";

        @Required private final String system;

        public HasSystem(String system) {
            this.system = system;
        }

        private HasSystem() { this(null); }

        @Override public String type() { return TYPE; }

        public String system() { return system; }

        @Override
        public boolean applies(TreeNode tree) {
            return tree.value().baseSystem(system) != null;
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String toString() {
            return TYPE + ":" + system;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HasSystem hasSystem = (HasSystem) o;
            return system.equals(hasSystem.system);
        }

        @Override
        public int hashCode() {
            return Objects.hash(system);
        }
    }
}
