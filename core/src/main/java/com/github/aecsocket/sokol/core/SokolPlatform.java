package com.github.aecsocket.sokol.core;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.sokol.core.registry.Registry;

public interface SokolPlatform {
    I18N i18n();

    Registry<? extends SokolComponent.Scoped<?, ?, ?>> components();

    Registry<? extends Feature<?, ?>> features();

    interface Scoped<
        C extends SokolComponent.Scoped<C, ?, ?>,
        F extends Feature<?, ?>
    > extends SokolPlatform {
        @Override Registry<C> components();
        @Override Registry<F> features();
    }

    /*I18N i18n();

    interface Scoped<
            C extends Component.Scoped<C, ?, ?>,
            B extends BlueprintNode.Scoped<B, ?, ?, ?>,
            F extends Feature<?>
    > extends SokolPlatform {
        Registry<C> components();

        Registry<KeyedBlueprint<B>> blueprints();

        Registry<F> features();

        void setUpSerializers(Map<String, Stat<?>> statTypes, Map<String, Class<? extends NodeRule>> ruleTypes);

        void tearDownSerializers();
    }*/
}
