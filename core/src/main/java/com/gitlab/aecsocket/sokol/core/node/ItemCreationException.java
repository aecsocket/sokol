package com.gitlab.aecsocket.sokol.core.node;

public class ItemCreationException extends RuntimeException {
    public ItemCreationException() {}
    public ItemCreationException(String message) { super(message); }
    public ItemCreationException(String message, Throwable cause) { super(message, cause); }
    public ItemCreationException(Throwable cause) { super(cause); }
}
