package com.github.aecsocket.sokol.core.stat;

public class StatAccessException extends RuntimeException {
    public StatAccessException() {}

    public StatAccessException(String message) {
        super(message);
    }

    public StatAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatAccessException(Throwable cause) {
        super(cause);
    }
}
