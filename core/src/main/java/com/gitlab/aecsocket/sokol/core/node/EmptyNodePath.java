package com.gitlab.aecsocket.sokol.core.node;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;

/* package */ final class EmptyNodePath implements NodePath {
    public static final EmptyNodePath INSTANCE = new EmptyNodePath();

    private static final String[] array = new String[0];

    @Override public int size() { return 0; }
    @Override public String get(int idx) { throw new IndexOutOfBoundsException(); }
    @Override public @Nullable String last() { return null; }

    @Override public List<String> list() { return Collections.emptyList(); }
    @Override public String[] array() { return array; }

    @Override public String toString() { return "[]"; }

    @Override public boolean equals(Object obj) { return obj instanceof EmptyNodePath; }
    @Override public int hashCode() { return getClass().hashCode(); }
}
