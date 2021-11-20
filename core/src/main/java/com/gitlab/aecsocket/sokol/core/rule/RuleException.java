package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Renderable;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Locale;

public class RuleException extends IncompatibilityException implements Renderable {
    private final Rule rule;
    private final String lcKey;
    private final Object[] lcArgs;

    public RuleException(Rule rule, String lcKey, Object... lcArgs) {
        this.rule = rule;
        this.lcKey = lcKey;
        this.lcArgs = lcArgs;
    }

    public RuleException(Rule rule, Throwable cause, String lcKey, Object... lcArgs) {
        super(cause);
        this.rule = rule;
        this.lcKey = lcKey;
        this.lcArgs = lcArgs;
    }

    public Rule rule() { return rule; }
    public String lcKey() { return lcKey; }
    public Object[] lcArgs() { return lcArgs; }

    @Override
    public Component render(Locale locale, Localizer lc) {
        Object[] lcArgs = this.lcArgs;
        if (getCause() instanceof RuleException ruleCause) {
            int len = lcArgs.length;
            lcArgs = Arrays.copyOfRange(lcArgs, 0, len + 2);
            lcArgs[len] = "cause";
            lcArgs[len+1] = ruleCause.render(locale, lc);
        }
        return lc.safe(locale, "rule.incompatibility." + lcKey, lcArgs);
    }
}
