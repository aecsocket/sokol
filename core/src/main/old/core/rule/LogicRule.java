package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Tree;
import net.kyori.adventure.text.Component;

import java.util.*;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;
import static com.gitlab.aecsocket.sokol.core.rule.Rule.*;

public final class LogicRule {
    private LogicRule() {}

    public static final class Not implements Rule {
        private final Rule term;

        public Not(Rule term) {
            this.term = term;
        }

        public Rule term() { return term; }

        @Override
        public void applies(Node node, Tree<?> tree) throws RuleException {
            try {
                term.applies(node, tree);
                throw new RuleException(this);
            } catch (RuleException ignore) {}
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
            term.visit(visitor);
        }

        @Override
        public String name() { return "not"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Not not = (Not) o;
            return term.equals(not.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(term);
        }

        @Override
        public String toString() { return "!(" + term + ")"; }

        @Override
        public Component render(Locale locale, Localizer lc) {
            return text("!", OPERATOR)
                    .append(wrapBrackets(term.render(locale, lc)));
        }
    }

    public static abstract class HasTerms implements Rule {
        protected final List<Rule> terms;

        public HasTerms(List<Rule> terms) {
            this.terms = terms;
        }

        public List<Rule> terms() { return terms; }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
            for (var term : terms)
                term.visit(visitor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HasTerms hasTerms = (HasTerms) o;
            return terms.equals(hasTerms.terms);
        }

        @Override
        public int hashCode() {
            return Objects.hash(terms);
        }

        protected abstract String symbol();

        @Override
        public String toString() {
            StringJoiner result = new StringJoiner(" " + symbol() + " ");
            for (var term : terms)
                result.add("(" + term.toString() + ")");
            return result.toString();
        }

        @Override
        public Component render(Locale locale, Localizer lc) {
            List<Component> components = new ArrayList<>();
            for (var term : terms)
                components.add(wrapBrackets(term.render(locale, lc)));
            return join(separator(text(" " + symbol() + " ", SYMBOL)), components);
        }
    }

    public static final class And extends HasTerms {
        public And(List<Rule> terms) {
            super(terms);
        }

        @Override
        public void applies(Node node, Tree<?> tree) throws RuleException {
            for (var term : terms) {
                try {
                    term.applies(node, tree);
                } catch (RuleException e) {
                    throw new RuleException(this, e);
                }
            }
        }

        @Override
        public String name() { return "and"; }

        @Override
        protected String symbol() { return "&"; }
    }

    public static final class Or extends HasTerms {
        public Or(List<Rule> terms) {
            super(terms);
        }

        @Override
        public void applies(Node node, Tree<?> tree) throws RuleException {
            for (var term : terms) {
                try {
                    term.applies(node, tree);
                    return;
                } catch (RuleException ignore) {}
            }
            throw new RuleException(this, "or");
        }

        @Override
        public String name() { return "or"; }

        @Override
        protected String symbol() { return "|"; }
    }
}