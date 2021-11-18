package com.gitlab.aecsocket.sokol.core.node;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;

/* package */ final class ListNodePath implements NodePath {
    private final List<String> list;
    private @Nullable String[] array;

    public ListNodePath(List<String> list) {
        this.list = list;
    }

    @Override public int size() { return list.size(); }
    @Override public String get(int idx) { return list.get(idx); }
    @Override public @Nullable String last() { return list.size() == 0 ? null : list.get(list.size() - 1); }

    @Override public List<String> list() { return list; }
    @Override public String[] array() { return array == null ? array = list.toArray(new String[0]) : array; }

    @Override public String toString() { return list.toString(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListNodePath that = (ListNodePath) o;
        return list.equals(that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list);
    }
}
