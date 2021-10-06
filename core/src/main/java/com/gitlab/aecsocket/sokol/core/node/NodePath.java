package com.gitlab.aecsocket.sokol.core.node;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface NodePath {
    static NodePath empty() {
        return new NodePath() {
            private static final String[] array = new String[0];
            @Override public int size() { return 0; }
            @Override public String get(int idx) { throw new IndexOutOfBoundsException(); }
            @Override public @Nullable String last() { return null; }
            @Override public List<String> list() { return Collections.emptyList(); }
            @Override public String[] array() { return array; }
        };
    }

    static NodePath path(List<String> path) {
        return new NodePath() {
            private String[] array;
            @Override public int size() { return path.size(); }
            @Override public String get(int idx) { return path.get(idx); }
            @Override public @Nullable String last() { return path.size() == 0 ? null : path.get(path.size() - 1); }
            @Override public List<String> list() { return path; }
            @Override public String[] array() { return array == null ? array = path.toArray(new String[0]) : array; }
        };
    }

    static NodePath path(String[] path) {
        return new NodePath() {
            private List<String> list;
            @Override public int size() { return path.length; }
            @Override public String get(int idx) { return path[idx]; }
            @Override public @Nullable String last() {return path.length == 0 ? null : path[path.length - 1];}
            @Override public List<String> list() { return list == null ? list = Arrays.asList(path) : list; }
            @Override public String[] array() { return path; }
        };
    }

    int size();
    String get(int idx);
    @Nullable String last();

    List<String> list();
    String[] array();
}
