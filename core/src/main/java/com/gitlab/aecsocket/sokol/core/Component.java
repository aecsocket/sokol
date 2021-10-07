package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Component extends Keyed {
    Set<String> tags();
    boolean tagged(String tag);

    Map<String, ? extends Slot> slots();
    Optional<? extends Slot> slot(String key);

    Map<String, ? extends Feature<?, ?>> featureTypes();
    Optional<? extends Feature<?, ?>> featureType(String key);

    interface Scoped<
            C extends Scoped<C, S, F, N>,
            S extends Slot,
            F extends Feature<? extends FeatureInstance<? extends N>, N>,
            N extends Node.Scoped<N, ?, ?>
    > extends Component {
        @Override Map<String, S> slots();
        @Override Optional<S> slot(String key);

        @Override Map<String, F> featureTypes();
        @Override Optional<F> featureType(String key);
    }
}
