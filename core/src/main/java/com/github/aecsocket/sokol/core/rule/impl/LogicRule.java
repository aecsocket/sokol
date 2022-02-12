package com.github.aecsocket.sokol.core.rule.impl;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.sokol.core.SokolNode;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.rule.RuleException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import static net.kyori.adventure.text.Component.*;

public final class LogicRule {
    private LogicRule() {}

    public static final class Constant implements Rule {
        public static final Constant TRUE = new Constant(true);
        public static final Constant FALSE = new Constant(false);
        public static final String
            RULE_CONSTANT_TRUE = "rule.constant.true",
            RULE_CONSTANT_FALSE = "rule.constant.false";

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

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, value ? RULE_CONSTANT_TRUE : RULE_CONSTANT_FALSE);
        }
    }

    public static final class Not implements Rule {
        public static final String RULE_NOT = "rule.not";

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

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_NOT,
                c -> c.of("term", () -> c.rd(term)));
        }
    }

    public interface WithTerms extends Rule {
        String RULE_WITH_TERMS_RESULT = "rule.with_terms.result";
        String RULE_WITH_TERMS_TERM = "rule.with_terms.term";

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

        protected abstract Component separator(I18N i18n, Locale locale);

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_WITH_TERMS_RESULT,
                c -> c.of("terms", () -> join(JoinConfiguration.separator(separator(i18n, locale)),
                    terms.stream()
                        .map(rule -> c.line(RULE_WITH_TERMS_TERM,
                            d -> d.of("term", () -> d.rd(rule))))
                        .toList()
                )));
        }
    }
    
    public static final class And extends WithTermsImpl {
        public static final String RULE_AND_SEPARATOR = "rule.and.separator";

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

        @Override
        protected Component separator(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_AND_SEPARATOR);
        }
    }
    
    public static final class Or extends WithTermsImpl {
        public static final String RULE_OR_SEPARATOR = "rule.or.separator";

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

        @Override
        protected Component separator(I18N i18n, Locale locale) {
            return i18n.line(locale, RULE_OR_SEPARATOR);
        }
    }
}
