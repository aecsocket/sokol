package com.github.aecsocket.sokol.core;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;

public interface SokolComponent extends Keyed {
    Set<String> tags();
    boolean tagged(String key);

    Map<String, ? extends NodeSlot> slots();
    Optional<? extends NodeSlot> slot(String key);

    Map<String, ? extends FeatureProfile<?, ?, ?>> features();
    Optional<? extends FeatureProfile<?, ?, ?>> feature(String key);

    StatIntermediate stats();

    interface Scoped<
        C extends Scoped<C, S, P>,
        S extends NodeSlot.Scoped<S, C>,
        P extends FeatureProfile<P, ?, ?>
    > extends SokolComponent {
        @Override Map<String, S> slots();
        @Override Optional<S> slot(String key);
    
        @Override Map<String, P> features();
        @Override Optional<P> feature(String key);
    }
}
