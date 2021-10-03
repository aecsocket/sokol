package com.gitlab.aecsocket.sokol.core.registry;

public final class ValidationException extends RuntimeException {
    private final int index;
    private final char invalid;

    public ValidationException(int index, char invalid) {
        super("Invalid character at index " + index + ": found '" + invalid + "'");
        this.index = index;
        this.invalid = invalid;
    }

    public int index() { return index; }
    public char invalid() { return invalid; }
}
