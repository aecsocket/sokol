package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Tree;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;
import static com.gitlab.aecsocket.sokol.core.rule.Rule.*;

public final class NavigationRule {
    private NavigationRule() {}

    private static Component formatPath(NodePath path) {
        return join(separator(text("/", PATH)),
                path.list().stream().map(p -> text(p, PATH)).collect(Collectors.toList()));
    }

    public static final class Has implements Rule {
        private final NodePath path;

        public Has(NodePath path) {
            this.path = path;
        }

        public NodePath path() { return path; }

        @Override
        public void applies(Node node, Tree<?> tree) throws RuleException {
            if (node.node(path).isEmpty())
                throw new RuleException(this);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String name() { return "has"; }

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

        @Override
        public Component render(Locale locale, Localizer lc) {
            return text("?", OPERATOR)
                    .append(formatPath(path));
        }
    }

    public static abstract class Navigation implements Rule {
        protected final NodePath path;
        protected final Rule term;

        public Navigation(NodePath path, Rule term) {
            this.path = path;
            this.term = term;
        }

        public NodePath path() { return path; }
        public Rule term() { return term; }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
            term.visit(visitor);
        }

        protected abstract String symbol();

        @Override
        public String toString() { return symbol() + path + "(" + term + ")"; }

        @Override
        public Component render(Locale locale, Localizer lc) {
            return text(symbol(), OPERATOR)
                    .append(formatPath(path))
                    .append(wrapBrackets(term.render(locale, lc)));
        }
    }

    public static final class As extends Navigation {
        public As(NodePath path, Rule term) {
            super(path, term);
        }

        @Override
        public void applies(Node node, Tree<?> tree) throws RuleException {
            var child = node.node(path);
            if (child.isEmpty())
                throw new RuleException(this);
            try {
                term.applies(child.get(), tree);
            } catch (RuleException e) {
                throw new RuleException(this);
            }
        }

        @Override
        public String name() { return "as"; }

        @Override
        protected String symbol() { return "$"; }

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
    }

    public static final class AsRoot extends Navigation {
        public AsRoot(NodePath path, Rule term) {
            super(path, term);
        }

        @Override
        public void applies(Node node, Tree<?> tree) throws RuleException {
            var child = node.root().node(path);
            if (child.isEmpty())
                throw new RuleException(this);
            try {
                term.applies(child.get(), tree);
            } catch (RuleException e) {
                throw new RuleException(this, e);
            }
        }

        @Override
        public String name() { return "as_root"; }

        @Override
        protected String symbol() { return "/"; }

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
    }

    public static final class IsRoot implements Rule {
        public static final IsRoot INSTANCE = new IsRoot();

        private IsRoot() {}

        @Override
        public void applies(Node node, Tree<?> tree) throws RuleException {
            if (!node.isRoot())
                throw new RuleException(this, "not_root");
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public String name() { return "is_root"; }

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

        @Override
        public Component render(Locale locale, Localizer lc) {
            return text("/?", OPERATOR);
        }
    }
}
