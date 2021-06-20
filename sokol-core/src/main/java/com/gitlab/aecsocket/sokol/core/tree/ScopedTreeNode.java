package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ScopedTreeNode<N extends ScopedTreeNode<N, C, S, B, Y>, C extends Component.Scoped<C, S, B>, S extends Slot, B extends System<N>, Y extends System.Instance<N>>
        extends TreeNode {

    @Override @NotNull C value();
    @Override @NotNull EventDispatcher<TreeEvent> events();

    @Override @NotNull Map<String, N> children();

    @Override N child(String key);
    @NotNull N child(String key, N child);

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
