package com.github.aecsocket.sokol.core;

import com.github.aecsocket.sokol.core.api.Blueprint;
import com.github.aecsocket.sokol.core.api.Component;
import com.github.aecsocket.sokol.core.api.Feature;
import com.github.aecsocket.sokol.core.impl.KeyedBlueprint;
import com.github.aecsocket.sokol.core.registry.Registry;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.minecommons.core.i18n.I18N;

import java.util.Map;

public interface SokolPlatform {
    I18N i18n();

    interface Scoped<
            C extends Component.Scoped<C, ?, ?>,
            B extends Blueprint.Scoped<B, ?, ?, ?>,
            F extends Feature<?>
    > extends SokolPlatform {
        Registry<C> components();

        Registry<KeyedBlueprint<B>> blueprints();

        Registry<F> features();

        void setUpSerializers(Map<String, Stat<?>> statTypes, Map<String, Class<? extends NodeRule>> ruleTypes);

        void tearDownSerializers();
    }
}
