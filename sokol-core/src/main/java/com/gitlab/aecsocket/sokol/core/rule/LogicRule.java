package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.List;
import java.util.Objects;

public final class LogicRule {
    private LogicRule() {}

    @ConfigSerializable
    public static final class Not implements Rule {
        public static final String TYPE = "not";

        @Required private final Rule term;

        public Not(Rule term) {
            this.term = term;
        }

        private Not() { this(null); }

        @Override public String type() { return TYPE; }

        public Rule term() { return term; }

        @Override
        public boolean applies(TreeNode node) {
            return !term.applies(node);
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
            Not not = (Not) o;
            return term.equals(not.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(term);
        }

        @Override
        public String toString() { return "!<" + term + ">"; }
    }

    public static abstract class HasTerms implements Rule {
        @Required protected final List<Rule> terms;

        public HasTerms(List<Rule> terms) {
            this.terms = terms;
        }

        protected HasTerms() { this(null); }

        public List<Rule> terms() { return terms; }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
            for (Rule term : terms) {
                term.visit(visitor);
            }
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

        @Override
        public String toString() { return "<" + terms + ">"; }
    }

    @ConfigSerializable
    public static final class And extends HasTerms {
        public static final String TYPE = "and";

        public And(List<Rule> terms) {
            super(terms);
        }

        private And() {}

        @Override public String type() { return TYPE; }

        @Override
        public boolean applies(TreeNode node) {
            for (Rule rule : terms) {
                if (!rule.applies(node)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() { return "&" + super.toString(); }
    }

    @ConfigSerializable
    public static final class Or extends HasTerms {
        public static final String TYPE = "or";

        public Or(List<Rule> terms) {
            super(terms);
        }

        private Or() {}

        @Override public String type() { return TYPE; }

        @Override
        public boolean applies(TreeNode node) {
            for (Rule rule : terms) {
                if (rule.applies(node)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() { return "|" + super.toString(); }
    }
}
