package com.github.aecsocket.sokol.core;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.registry.Registry;
import com.github.aecsocket.sokol.core.registry.ValidationException;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.stat.Stat;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Map;

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

        void setUpSerializers(Map<String, Stat<?>> statTypes, Map<String, Class<? extends Rule>> ruleTypes);

        void tearDownSerializers();
    }

    static String idByValue(Type type, ConfigurationNode node) throws SerializationException {
        String id = Serializers.require(node, String.class);
        try {
            Keyed.validate(id);
        } catch (ValidationException e) {
            throw new SerializationException(node, type, "Invalid ID `" + id + "`");
        }
        return id;
    }

    static String idByKey(Type type, ConfigurationNode node) throws SerializationException {
        String id = ""+node.key();
        try {
            Keyed.validate(id);
        } catch (ValidationException e) {
            throw new SerializationException(node, type, "Invalid ID `" + id + "`");
        }
        return id;
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
