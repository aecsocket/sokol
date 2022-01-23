package com.gitlab.aecsocket.paper;

public class PersistenceException extends Exception {
    public PersistenceException() {}

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceException(Throwable cause) {
        super(cause);
    }
}
