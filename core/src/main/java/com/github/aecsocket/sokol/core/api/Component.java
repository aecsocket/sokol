package com.github.aecsocket.sokol.core.api;

import java.util.*;

import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;

public interface Component extends Keyed {
    Set<String> tags();
    boolean tagged(String tag);

    Map<String, ? extends NodeSlot> slots();
    Optional<? extends NodeSlot> slot(String key);

    Map<String, ? extends FeatureConfig<?, ?, ?, ?>> features();
    Optional<? extends FeatureConfig<?, ?, ?, ?>> feature(String key);

    StatIntermediate stats();

    interface Scoped<
            C extends Scoped<C, S, G>,
            S extends NodeSlot,
            G extends FeatureConfig<?, ?, ?, ?>
    > extends Component {
        @Override Map<String, S> slots();
        @Override Optional<S> slot(String key);

        @Override Map<String, G> features();
        @Override Optional<G> feature(String key);
    }
}
