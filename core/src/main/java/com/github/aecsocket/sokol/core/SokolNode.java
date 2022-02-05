package com.github.aecsocket.sokol.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.node.MapNode;
import com.github.aecsocket.minecommons.core.node.NodePath;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface SokolNode extends MapNode {
    SokolComponent value();

    boolean hasFeature(String key);
    Optional<? extends FeatureData<?, ?, ?, ?>> featureData(String key);

    boolean complete();


    @Override @Nullable SokolNode parent();
    @Override SokolNode root();

    @Override
    Map<String, ? extends SokolNode> children();
    @Override
    Collection<? extends SokolNode> childValues();

    @Override Optional<? extends SokolNode> get(NodePath path);
    @Override Optional<? extends SokolNode> get(String... path);
    @Override Optional<? extends SokolNode> get(String path);

    void visitSokolNodes(Consumer<SokolNode> visitor);

    @Override SokolNode copy();
    @Override SokolNode asRoot();

    static boolean complete(SokolNode node) {
        AtomicBoolean result = new AtomicBoolean(true);
        node.visitSokolNodes(child -> {
            if (!result.get())
                return;
            for (var entry : child.value().slots().entrySet()) {
                if (entry.getValue().required() && !child.has(entry.getKey())) {
                    result.set(false);
                    return;
                }
            }
        });
        return result.get();
    }
}
