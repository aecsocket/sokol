package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface TreeNode {
    record ChildSlot<S, N>(S slot, N child) {}

    interface Visitor<N extends TreeNode> {
        void visit(N node, String... path);
    }

    @NotNull Component value();
    @NotNull EventDispatcher<TreeEvent> events();
    @NotNull StatMap stats();

    @NotNull Map<String, ? extends TreeNode> children();
    TreeNode child(String key);

    @NotNull Map<String, ? extends ChildSlot<?, ?>> slotChildren();

    @NotNull Map<String, ? extends System.Instance<?>> systems();
    System.Instance<?> system(String id);

    @NotNull TreeNode build();
    void visit(Visitor<TreeNode> visitor, String... path);

    String key();
    TreeNode parent();
    Slot slot();
    String[] path();

    TreeNode root();
    default boolean isRoot() { return key() == null; }
}
