package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.node.RuleException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;

import java.util.Objects;

public final class NavigationRule {
    private NavigationRule() {}

    public static final class Has implements Rule {
        private final NodePath path;

        public Has(NodePath path) {
            this.path = path;
        }

        public NodePath path() { return path; }

        @Override
        public void applies(Node node) throws RuleException {
            if (node.node(path).isEmpty())
                throw new RuleException(this, "No node at " + path);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Has has = (Has) o;
            return path.equals(has.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        @Override
        public String toString() { return "?" + path; }
    }

    public static final class As implements Rule {
        private final NodePath path;
        private final Rule term;

        public As(NodePath path, Rule term) {
            this.path = path;
            this.term = term;
        }

        public NodePath path() { return path; }
        public Rule term() { return term; }

        @Override
        public void applies(Node node) throws RuleException {
            var child = node.node(path);
            if (child.isEmpty())
                throw new RuleException(this, "No node at " + path);
            try {
                term.applies(child.get());
            } catch (RuleException e) {
                throw new RuleException(this, "At " + path, e);
            }
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
            return path.equals(as.path) && term.equals(as.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, term);
        }

        @Override
        public String toString() { return "$" + path + "(" + term + ")"; }
    }

    public static final class AsRoot implements Rule {
        private final NodePath path;
        private final Rule term;

        public AsRoot(NodePath path, Rule term) {
            this.path = path;
            this.term = term;
        }

        public NodePath path() { return path; }
        public Rule term() { return term; }

        @Override
        public void applies(Node node) throws RuleException {
            var child = node.root().node(path);
            if (child.isEmpty())
                throw new RuleException(this, "No node from root at " + path);
            try {
                term.applies(child.get());
            } catch (RuleException e) {
                throw new RuleException(this, "As root", e);
            }
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
            AsRoot asRoot = (AsRoot) o;
            return term.equals(asRoot.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(term);
        }

        @Override
        public String toString() { return "/(" + term + ")"; }
    }

    public static final class IsRoot implements Rule {
        public static final IsRoot INSTANCE = new IsRoot();

        private IsRoot() {}

        @Override
        public void applies(Node node) throws RuleException {
            if (!node.isRoot())
                throw new RuleException(this);
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
        public String toString() { return "/?"; }
    }
}
