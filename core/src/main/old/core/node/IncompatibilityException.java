package com.gitlab.aecsocket.sokol.core.node;

import org.jetbrains.annotations.Nullable;

public class IncompatibilityException extends Exception {
    public IncompatibilityException() {}

    public IncompatibilityException(@Nullable String message) {
        super(message);
    }

    public IncompatibilityException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public IncompatibilityException(@Nullable Throwable cause) {
        super(cause);
    }
}
