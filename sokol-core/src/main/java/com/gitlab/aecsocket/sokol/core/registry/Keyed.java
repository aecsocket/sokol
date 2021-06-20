package com.gitlab.aecsocket.sokol.core.registry;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public interface Keyed {
    String VALID_KEY = "[a-z0-9/._-]+";
    Pattern VALID_KEY_PATTERN = Pattern.compile(VALID_KEY);

    static boolean validKey(String key) {
        return VALID_KEY_PATTERN.matcher(key).matches();
    }

    @NotNull String id();
}
