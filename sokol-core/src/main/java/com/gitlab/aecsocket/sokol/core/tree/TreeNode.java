package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface TreeNode {
    interface Scoped<N extends Scoped<N, C, S, B, Y>, C extends Component.Scoped<C, S, B>, S extends Slot, B extends System<N>, Y extends System.Instance<N>>
            extends TreeNode {

        @Override @NotNull C value();
        @Override @NotNull EventDispatcher<TreeEvent> events();

        @Override @NotNull Map<String, N> children();

        @Override N child(String key);
        @NotNull N child(String key, N child);

        @Override N node(String... path);

        @Override @NotNull Map<String, ChildSlot<S, N>> slotChildren();

        @Override @NotNull Map<String, Y> systems();
        @Override Y system(String id);
        @SuppressWarnings("unchecked")
        default <T extends Y> T systemOf(String id) { return (T) system(id); }
        void system(Y system);

        @NotNull N build();
        void visitScoped(Visitor<N> visitor, String... path);

        @Override N parent();
        @Override S slot();

        @Override N root();
    }

    record ChildSlot<S, N>(S slot, N child) {}

    interface Visitor<N extends TreeNode> {
        void visit(N node, String... path);
    }

    @NotNull Component value();
    @NotNull EventDispatcher<TreeEvent> events();
    @NotNull StatMap stats();
    boolean complete();

    @NotNull Map<String, ? extends TreeNode> children();
    TreeNode child(String key);

    TreeNode node(String... path);

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
