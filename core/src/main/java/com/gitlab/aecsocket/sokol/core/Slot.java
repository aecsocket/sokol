package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.util.IncompatibilityException;

import java.util.Set;

public interface Slot {
    Component parent();
    String key();

    Set<String> tags();
    boolean tagged(String tag);

    void compatibility(Node parent, Node child) throws IncompatibilityException;
}
