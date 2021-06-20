package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.sokol.core.system.System;

public interface TreeEvent {
    TreeNode node();

    interface SystemEvent<Y extends System.Instance<?>> extends TreeEvent {
        Y system();

        @Override default TreeNode node() { return system().parent(); }
    }
}
