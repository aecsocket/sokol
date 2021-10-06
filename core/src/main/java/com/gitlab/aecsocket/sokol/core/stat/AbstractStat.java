package com.gitlab.aecsocket.sokol.core.stat;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public abstract class AbstractStat<T> implements Stat<T> {
    private final String key;
    private final @Nullable T defaultValue;

    public AbstractStat(String key, @Nullable T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String key() { return key; }
    public Optional<T> defaultValue() { return Optional.ofNullable(defaultValue); }
}
