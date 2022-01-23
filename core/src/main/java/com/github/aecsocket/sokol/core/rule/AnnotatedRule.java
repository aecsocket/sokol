package com.github.aecsocket.sokol.core.rule;

import java.util.Map;

public interface AnnotatedRule<R extends Rule> extends Rule {
    R rule();
    String key();
    Map<String, String> args();
}
