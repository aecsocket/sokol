package com.gitlab.aecsocket.sokol.core.node;

public class IncompatibilityException extends Exception {
    public IncompatibilityException() {}
    public IncompatibilityException(String message) { super(message); }
    public IncompatibilityException(String message, Throwable cause) { super(message, cause); }
    public IncompatibilityException(Throwable cause) { super(cause); }
}
