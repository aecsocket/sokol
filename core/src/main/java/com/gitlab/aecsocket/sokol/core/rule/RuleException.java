package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RuleException extends IncompatibilityException {
    private final Rule rule;
    private final List<String> path = new ArrayList<>();

    public RuleException(Rule rule, @Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.rule = rule;
        for (Throwable cur = this; cur != null; cur = cur.getCause()) {
            if (cur instanceof RuleException ruleEx)
                path.add(0, ruleEx.rule.name());
        }
    }

    public RuleException(Rule rule) {
        this(rule, null, null);
    }

    public RuleException(Rule rule, String message) {
        this(rule, message, null);
    }

    public RuleException(Rule rule, Throwable cause) {
        this(rule, null, cause);
    }

    public Rule rule() { return rule; }

    public String rawMessage() { return super.getMessage(); }

    @Override
    public String getMessage() {
        return "[" + String.join(", ", path) + "]: " + rawMessage();
    }
}
