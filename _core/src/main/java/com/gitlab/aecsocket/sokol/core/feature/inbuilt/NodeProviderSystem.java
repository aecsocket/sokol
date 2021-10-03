package com.gitlab.aecsocket.sokol.core.feature.inbuilt;

import com.gitlab.aecsocket.sokol.core.feature.Feature;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;

import java.util.Optional;

public interface NodeProviderSystem extends Feature.Instance {
    Optional<? extends TreeNode> peek();
    Optional<? extends TreeNode> pop();
}
