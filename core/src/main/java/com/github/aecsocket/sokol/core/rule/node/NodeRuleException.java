package com.github.aecsocket.sokol.core.rule.node;

import com.github.aecsocket.sokol.core.api.IncompatibleException;

public class NodeRuleException extends IncompatibleException {
    private final NodeRule rule;

    public NodeRuleException(NodeRule rule) {
        this.rule = rule;
    }

    public NodeRuleException(String message, NodeRule rule) {
        super(message);
        this.rule = rule;
    }

    public NodeRuleException(String message, Throwable cause, NodeRule rule) {
        super(message, cause);
        this.rule = rule;
    }

    public NodeRuleException(Throwable cause, NodeRule rule) {
        super(cause);
        this.rule = rule;
    }

    public NodeRule rule() { return rule; }
}
