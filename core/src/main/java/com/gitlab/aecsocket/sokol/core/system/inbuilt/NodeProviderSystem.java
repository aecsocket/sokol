package com.gitlab.aecsocket.sokol.core.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;

import java.util.Optional;

public interface NodeProviderSystem extends System.Instance {
    Optional<? extends TreeNode> peek();
    Optional<? extends TreeNode> pop();
}
