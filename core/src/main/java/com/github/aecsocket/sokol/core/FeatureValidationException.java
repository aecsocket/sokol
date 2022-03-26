package com.github.aecsocket.sokol.core;

public class FeatureValidationException extends Exception {
    public FeatureValidationException() {}

    public FeatureValidationException(String message) {
        super(message);
    }

    public FeatureValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeatureValidationException(Throwable cause) {
        super(cause);
    }
}
