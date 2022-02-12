package com.github.aecsocket.sokol.core.rule.impl;

import java.util.Locale;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.SokolNode;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.rule.RuleException;
import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.Component.*;

public final class NavRule {
    private NavRule() {}

    public static final class Has implements Rule {
        public static final String RULE_HAS = "rule.has";

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

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_HAS,
                c -> c.of("path", () -> text(String.join("/", path))));
        }
    }

    public static final class As implements Rule {
        public static final String RULE_AS = "rule.as";

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

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_AS,
                c -> c.of("path", () -> text(String.join("/", path))),
                c -> c.of("term", () -> c.rd(term)));
        }
    }

    public static final class AsRoot implements Rule {
        public static final String RULE_AS_ROOT = "rule.as_root";

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

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_AS_ROOT,
                    c -> c.of("path", () -> text(String.join("/", path))),
                    c -> c.of("term", () -> c.rd(term)));
        }
    }

    public static final class IsRoot implements Rule {
        public static final IsRoot INSTANCE = new IsRoot();
        public static final String RULE_IS_ROOT = "rule.is_root";

        private IsRoot() {}

        @Override
        public void applies(SokolNode target) throws RuleException {
            if (!target.isRoot())
                throw new RuleException();
        }

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_IS_ROOT);
        }
    }

    public static final class AsParent implements Rule.AcceptsParent {
        public static final String RULE_AS_PARENT = "rule.as_parent";

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

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_AS_PARENT,
                c -> c.of("term", () -> c.rd(term)));
        }
    }
}
