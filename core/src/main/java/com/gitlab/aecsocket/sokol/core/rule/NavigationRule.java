package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.Arrays;
import java.util.Objects;

/**
 * Rules which navigate the tree.
 */
public final class NavigationRule {
    private NavigationRule() {}

    /**
     * Gets if a node has a slot at the specified path filled.
     */
    @ConfigSerializable
    public static final class Has implements Rule {
        /** The rule type. */
        public static final String TYPE = "has";

        @Required private final String[] path;

        public Has(String[] path) {
            this.path = path;
        }

        @Override public String type() { return TYPE; }

        public String[] path() { return path; }

        @Override
        public boolean applies(TreeNode node) {
            return node.node(path).isPresent();
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Has as = (Has) o;
            return Arrays.equals(path, as.path);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(path);
        }

        @Override
        public String toString() { return "?" + Arrays.toString(path); }
    }

    /**
     * Evaluates another rule from a child node, specified by a path.
     */
    @ConfigSerializable
    public static final class As implements Rule {
        /** The rule type. */
        public static final String TYPE = "as";

        @Required private final String[] path;
        @Required private final Rule term;

        public As(String[] path, Rule term) {
            this.path = path;
            this.term = term;
        }

        @Override public String type() { return TYPE; }

        public String[] path() { return path; }
        public Rule term() { return term; }

        @Override
        public boolean applies(TreeNode node) {
            return node.node(path)
                    .map(term::applies)
                    .orElse(false);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
            term.visit(visitor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            As as = (As) o;
            return Arrays.equals(path, as.path) && term.equals(as.term);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(term);
            result = 31 * result + Arrays.hashCode(path);
            return result;
        }

        @Override
        public String toString() { return "$" + Arrays.toString(path) + "<" + term + ">"; }
    }

    /**
     * Evaluates another rule from a child of the root node, specified by a path.
     */
    @ConfigSerializable
    public static final class AsRoot implements Rule {
        /** The rule type. */
        public static final String TYPE = "as_root";

        @Required private final String[] path;
        @Required private final Rule term;

        public AsRoot(String[] path, Rule term) {
            this.path = path;
            this.term = term;
        }

        @Override public String type() { return TYPE; }

        public String[] path() { return path; }
        public Rule term() { return term; }

        @Override
        public boolean applies(TreeNode node) {
            return node.root().node(path)
                    .map(term::applies)
                    .orElse(false);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
            term.visit(visitor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AsRoot as = (AsRoot) o;
            return Arrays.equals(path, as.path) && term.equals(as.term);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(term);
            result = 31 * result + Arrays.hashCode(path);
            return result;
        }

        @Override
        public String toString() { return "/" + Arrays.toString(path) + "<" + term + ">"; }
    }

    /**
     * Gets if a node is the root node of its tree.
     */
    @ConfigSerializable
    public static final class IsRoot implements Rule {
        /** The rule type. */
        public static final String TYPE = "is_root";
        /** A pre-made instance. */
        public static final IsRoot INSTANCE = new IsRoot();

        private IsRoot() {}

        @Override
        public boolean applies(TreeNode node) {
            return node.isRoot();
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override public String type() { return TYPE; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() { return getClass().hashCode(); }

        @Override
        public String toString() { return "/?"; }
    }

    /**
     * Abstract rule for evaluating from nodes specified in code.
     */
    @ConfigSerializable
    public static abstract class AsInline implements Rule {
        private transient TreeNode node;
        @Required private final Rule term;

        public AsInline(Rule term) {
            this.term = term;
        }

        private AsInline() { this(Constant.FALSE); }

        public TreeNode node() { return node; }
        public void node(TreeNode node) { this.node = node; }

        public Rule term() { return term; }

        @Override
        public boolean applies(TreeNode node) {
            if (this.node == null)
                throw new IllegalStateException("Attempting to use inline node [rule type " + type() + "]");
            return term.applies(this.node);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
            term.visit(visitor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AsInline asInline = (AsInline) o;
            return Objects.equals(node, asInline.node) && term.equals(asInline.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, term);
        }

        @Override
        public String toString() { return type() + "<" + term + ">"; }
    }

    /**
     * Evaluates a rule from the child node in a slot compatibility check.
     */
    @ConfigSerializable
    public static final class AsChild extends AsInline {
        /** The rule type. */
        public static final String TYPE = "as_child";

        public AsChild(Rule term) {
            super(term);
        }

        private AsChild() {}

        @Override public String type() { return TYPE; }
    }

    /**
     * Evaluates a rule from the parent node in a slot compatibility check.
     */
    @ConfigSerializable
    public static final class AsParent extends AsInline {
        /** The rule type. */
        public static final String TYPE = "as_parent";

        public AsParent(Rule term) {
            super(term);
        }

        private AsParent() {}

        @Override public String type() { return TYPE; }
    }
}
