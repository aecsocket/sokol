package com.github.aecsocket.sokol.core.rule.impl;

import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.SokolNode;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.rule.RuleException;

public final class NavRule {
    private NavRule() {}

    public static final class Has implements Rule {
        private final NodePath path;

        public Has(NodePath path) {
            this.path = path;
        }

        public NodePath path() { return path; }

        @Override
        public void applies(SokolNode target) throws RuleException {
            if (!target.has(path))
                throw new RuleException();
        }
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
        public void applies(SokolNode target) throws RuleException {
            try {
                term.applies(target.get(path).orElseThrow(RuleException::new));
            } catch (RuleException e) {
                throw new RuleException(e);
            }
        }

        @Override
        public void visit(Consumer<Rule> visitor) {
            Rule.super.visit(visitor);
            term.visit(visitor);
        }
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
        public void applies(SokolNode target) throws RuleException {
            try {
                term.applies(target.root().get(path).orElseThrow(RuleException::new));
            } catch (RuleException e) {
                throw new RuleException(e);
            }
        }

        @Override
        public void visit(Consumer<Rule> visitor) {
            Rule.super.visit(visitor);
            term.visit(visitor);
        }
    }

    public static final class IsRoot implements Rule {
        public static final IsRoot INSTANCE = new IsRoot();

        private IsRoot() {}

        @Override
        public void applies(SokolNode target) throws RuleException {
            if (!target.isRoot())
                throw new RuleException();
        }
    }

    public static final class AsParent implements Rule.AcceptsParent {
        private final Rule term;
        private SokolNode node;

        public AsParent(Rule term) {
            this.term = term;
        }

        public Rule term() { return term; }

        @Override
        public void acceptParent(SokolNode node) { this.node = node; }

        @Override
        public void applies(SokolNode target) throws RuleException {
            try {
                term.applies(node);
            } catch (RuleException e) {
                throw new RuleException(e);
            }
        }

        @Override
        public void visit(Consumer<Rule> visitor) {
            AcceptsParent.super.visit(visitor);
            term.visit(visitor);
        }
    }
}
