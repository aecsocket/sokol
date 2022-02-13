package com.github.aecsocket.sokol.core;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;

public interface SokolComponent extends Keyed {
    String I18N_KEY = "component";

    Set<String> tags();
    boolean tagged(String key);

    Map<String, ? extends NodeSlot> slots();
    Optional<? extends NodeSlot> slot(String key);

    Map<String, ? extends FeatureProfile<?, ?>> features();
    Optional<? extends FeatureProfile<?, ?>> feature(String key);

    StatIntermediate stats();

    @Override default String i18nBase() { return I18N_KEY; }

    interface Scoped<
        C extends Scoped<C, S, P>,
        S extends NodeSlot.Scoped<S, C>,
        P extends FeatureProfile<?, ?>
    > extends SokolComponent {
        @Override Map<String, S> slots();
        @Override Optional<S> slot(String key);
    
        @Override Map<String, P> features();
        @Override Optional<P> feature(String key);
    }
}
