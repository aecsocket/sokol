package com.gitlab.aecsocket.sokol.core.node;

import com.gitlab.aecsocket.sokol.core.rule.Rule;

public class RuleException extends IncompatibilityException {
    private final Rule rule;

    public RuleException(Rule rule) {
        this.rule = rule;
    }

    public RuleException(Rule rule, String message) {
        super(message);
        this.rule = rule;
    }

    public RuleException(Rule rule, String message, Throwable cause) {
        super(message, cause);
        this.rule = rule;
    }

    public RuleException(Rule rule, Throwable cause) {
        super(cause);
        this.rule = rule;
    }

    public Rule rule() { return rule; }
}
