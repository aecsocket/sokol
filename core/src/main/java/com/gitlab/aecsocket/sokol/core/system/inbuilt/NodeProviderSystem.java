package com.gitlab.aecsocket.sokol.core.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;

import java.util.Optional;

public interface NodeProviderSystem<N extends TreeNode> {
    Optional<N> peek();
    Optional<N> pop();
}
