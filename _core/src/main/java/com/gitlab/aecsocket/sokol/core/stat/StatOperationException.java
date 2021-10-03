package com.gitlab.aecsocket.sokol.core.stat;

public class StatOperationException extends RuntimeException {
    public StatOperationException() {}
    public StatOperationException(String message) { super(message); }
    public StatOperationException(String message, Throwable cause) { super(message, cause); }
    public StatOperationException(Throwable cause) { super(cause); }
}
