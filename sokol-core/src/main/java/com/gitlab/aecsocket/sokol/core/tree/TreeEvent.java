package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.sokol.core.system.System;

public interface TreeEvent {
    TreeNode node();

    default boolean call() {
        node().events().call(this);
        if (this instanceof Cancellable cancellable)
            return cancellable.cancelled();
        return false;
    }

    interface SystemEvent<Y extends System.Instance> extends TreeEvent {
        Y system();

        @Override default TreeNode node() { return system().parent(); }
    }
}
