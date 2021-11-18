package com.gitlab.aecsocket.sokol.core.node;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.List;

/* package */ final class ArrayNodePath implements NodePath {
    private final String[] array;
    private @Nullable List<String> list;

    public ArrayNodePath(String[] array) {
        this.array = array;
    }

    @Override public int size() { return array.length; }
    @Override public String get(int idx) { return array[idx]; }
    @Override public @Nullable String last() { return array.length == 0 ? null : array[array.length - 1]; }

    @Override public List<String> list() { return list == null ? list = Arrays.asList(array) : list; }
    @Override public String[] array() { return array; }

    @Override public String toString() { return Arrays.toString(array); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayNodePath that = (ArrayNodePath) o;
        return Arrays.equals(array, that.array);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }
}
