package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;

public class RuleException extends IncompatibilityException {
    public RuleException(Rule rule, String lcKey, Object... lcArgs) {
        super(rule, lcKey, lcArgs);
    }

    public RuleException(Rule rule, Throwable cause, String lcKey, Object... lcArgs) {
        super(rule, cause, lcKey, lcArgs);
    }
}
