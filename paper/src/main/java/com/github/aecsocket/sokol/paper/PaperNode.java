package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.SokolNode;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface PaperNode extends SokolNode {
    @Override Optional<? extends PaperFeatureData<?, ?>> featureData(String key);


    @Override @Nullable PaperNode parent();
    @Override PaperNode root();

    @Override Map<String, ? extends PaperNode> children();
    @Override Collection<? extends PaperNode> childValues();

    @Override Optional<? extends PaperNode> get(NodePath path);
    @Override Optional<? extends PaperNode> get(String... path);
    @Override Optional<? extends PaperNode> get(String path);

    @Override PaperNode copy();
    @Override PaperNode asRoot();
}
