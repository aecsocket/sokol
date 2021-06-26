package com.gitlab.aecsocket.sokol.core.registry;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * An object which has a string key, which conforms to {@link #VALID_KEY}.
 */
public interface Keyed {
    /**
     * The pattern that a valid ID must follow.
     * @see #VALID_KEY_PATTERN
     */
    String VALID_KEY = "[a-z0-9/._-]+";
    /**
     * The pattern that a valid ID must follow (compiled).
     * @see #VALID_KEY
     */
    Pattern VALID_KEY_PATTERN = Pattern.compile(VALID_KEY);

    /**
     * Checks if a passed key conforms to {@link #VALID_KEY}.
     * @param key The key.
     * @return The result.
     */
    static boolean validKey(@NotNull String key) {
        return VALID_KEY_PATTERN.matcher(key).matches();
    }

    @NotNull String id();
}
