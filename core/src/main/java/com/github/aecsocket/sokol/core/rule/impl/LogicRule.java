package com.github.aecsocket.sokol.core.rule.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.github.aecsocket.sokol.core.SokolNode;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.rule.RuleException;

public final class LogicRule {
    private LogicRule() {}

    public static final class Constant implements Rule {
        public static final Constant TRUE = new Constant(true);
        public static final Constant FALSE = new Constant(false);

        public static Constant of(boolean value) {
            return value ? TRUE : FALSE;
        }

        private final boolean value;

        private Constant(boolean value) {
            this.value = value;
        }

        public boolean value() { return value; }

        @Override
        public void applies(SokolNode target) throws RuleException {
            if (!value)
                throw new RuleException();
        }
    }

    public static final class Not implements Rule {
        private final Rule term;

        public Not(Rule term) {
            this.term = term;
        }

        public Rule term() { return term; }

        @Override
        public void applies(SokolNode target) throws RuleException {
            try {
                term.applies(target);
            } catch (RuleException e) {
                return;
            }
            throw new RuleException();
        }
    }

    public interface WithTerms extends Rule {
        List<Rule> terms();
    }

    private abstract static class WithTermsImpl implements WithTerms {
        protected final List<Rule> terms;

        public WithTermsImpl(List<Rule> terms) {
            this.terms = terms;
        }

        public List<Rule> terms() { return terms; }

        @Override
        public void visit(Consumer<Rule> visitor) {
            WithTerms.super.visit(visitor);
            for (var term : terms) {
                term.visit(visitor);
            }
        }
    }
    
    public static final class And extends WithTermsImpl {
        public And(List<Rule> terms) {
            super(terms);
        }

        @Override
        public void applies(SokolNode target) throws RuleException {
            for (var term : terms) {
                try {
                    term.applies(target);
                } catch (RuleException e) {
                    throw new RuleException(e);
                }
            }
        }
    }
    
    public static final class Or extends WithTermsImpl {
        public Or(List<Rule> terms) {
            super(terms);
        }

        @Override
        public void applies(SokolNode target) throws RuleException {
            for (var term : terms) {
                try {
                    term.applies(target);
                    return;
                } catch (RuleException ignore) {}
            }
            throw new RuleException();
        }
    }
}
