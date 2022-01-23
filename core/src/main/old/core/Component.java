package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;

import java.util.*;

public interface Component extends Keyed {
    Set<String> tags();
    boolean tagged(String tag);

    Map<String, ? extends Slot> slots();
    Optional<? extends Slot> slot(String key);

    Map<String, ? extends Feature<?, ?>> features();
    Optional<? extends Feature<?, ?>> feature(String key);

    StatIntermediate stats();

    static String renderKey(String id) { return "component." + id + ".name"; }
    static String renderDescriptionKey(String id) { return "component." + id + ".description"; }

    @Override
    default net.kyori.adventure.text.Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, renderKey(id()));
    }

    default Optional<List<net.kyori.adventure.text.Component>> renderDescription(Locale locale, Localizer lc) {
        return lc.lines(locale, renderDescriptionKey(id()));
    }

    interface Scoped<
            C extends Scoped<C, S, F, N>,
            S extends Slot,
            F extends Feature<? extends FeatureInstance<?, ? extends N>, N>,
            N extends Node.Scoped<N, ?, ?, ?, ?>
    > extends Component {
        @Override Map<String, S> slots();
        @Override Optional<S> slot(String key);

        @Override Map<String, F> features();
        @Override Optional<F> feature(String key);
    }
}
