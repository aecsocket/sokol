package com.gitlab.aecsocket.sokol.core.node;

public final class IncompatibilityException extends RuntimeException {
    public IncompatibilityException() {}
    public IncompatibilityException(String message) { super(message); }
    public IncompatibilityException(String message, Throwable cause) { super(message, cause); }
    public IncompatibilityException(Throwable cause) { super(cause); }
}
