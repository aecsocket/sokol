package com.github.aecsocket.sokol.core.rule.base;

import com.github.aecsocket.sokol.core.api.IncompatibleException;

public class BaseRuleException extends IncompatibleException {
    private final BaseRule rule;

    public BaseRuleException(BaseRule rule) {
        this.rule = rule;
    }

    public BaseRuleException(String message, BaseRule rule) {
        super(message);
        this.rule = rule;
    }

    public BaseRuleException(String message, Throwable cause, BaseRule rule) {
        super(message, cause);
        this.rule = rule;
    }

    public BaseRuleException(Throwable cause, BaseRule rule) {
        super(cause);
        this.rule = rule;
    }

    public BaseRule rule() { return rule; }
}
