package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface TreeNode {
    interface Scoped<N extends Scoped<N, C, S, B, Y>, C extends Component.Scoped<C, S, B>, S extends Slot, B extends System, Y extends System.Instance>
            extends TreeNode {

        @Override @NotNull C value();
        @Override @NotNull EventDispatcher<TreeEvent> events();

        @Override @NotNull Map<String, N> children();

        @Override Optional<N> child(String key);
        void child(String key, N child);

        @Override Optional<N> node(String... path);

        boolean combine(N node, boolean limited);

        @Override @NotNull Map<String, ChildSlot<S, N>> slotChildren();

        @Override @NotNull Map<String, Y> systems();
        <T extends Y> Optional<T> system(String id);
        void system(Y system);

        @NotNull N build();
        void visitScoped(Visitor<N, S> visitor, String... path);

        @Override Optional<N> parent();
        @Override Optional<S> slot();

        @Override @NotNull N root();
        @Override @NotNull N asRoot();
    }

    record ChildSlot<S extends Slot, N extends TreeNode>(S slot, Optional<N> child) {}

    interface Visitor<N extends TreeNode, S extends Slot> {
        void visit(N parent, S slot, Optional<? extends N> child, String... path);
    }

    @NotNull Component value();
    @NotNull EventDispatcher<TreeEvent> events();
    @NotNull StatMap stats();
    boolean complete();

    @NotNull Map<String, ? extends TreeNode> children();
    Optional<? extends TreeNode> child(String key);

    Optional<? extends TreeNode> node(String... path);

    @NotNull Map<String, ? extends ChildSlot<?, ?>> slotChildren();

    @NotNull Map<String, ? extends System.Instance> systems();

    @NotNull TreeNode build();
    void visit(Visitor<TreeNode, Slot> visitor, String... path);

    Optional<? extends TreeNode> parent();
    Optional<? extends Slot> slot();
    @NotNull String[] path();

    default boolean isRoot() { return slot().isEmpty(); }
    @NotNull TreeNode root();
    @NotNull TreeNode asRoot();
}
