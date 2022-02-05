package com.github.aecsocket.sokol.core.rule;

import com.github.aecsocket.sokol.core.IncompatibleException;

public class RuleException extends IncompatibleException {
    public RuleException() {}

    public RuleException(String message) {
        super(message);
    }

    public RuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleException(Throwable cause) {
        super(cause);
    }
}
