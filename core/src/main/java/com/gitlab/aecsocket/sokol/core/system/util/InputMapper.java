package com.gitlab.aecsocket.sokol.core.system.util;

import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

public class InputMapper extends EnumMap<InputType, Integer> {
    public static final class Loader {
        private final List<String> actionKeys = new ArrayList<>();

        private Loader() {}

        public Loader actionKeys(String... keys) { Collections.addAll(actionKeys, keys); return this; }

        public InputMapper loadFrom(ConfigurationNode node) throws SerializationException {
            InputMapper result = new InputMapper();
            var actionMap = Serializers.require(node, new TypeToken<Map<InputType, String>>() {});
            for (var entry : actionMap.entrySet()) {
                String actionKey = entry.getValue();
                int idx = actionKeys.indexOf(actionKey);
                if (idx == -1)
                    throw new SerializationException(node, InputMapper.class, "Invalid action [" + actionKey + "]");
                result.put(entry.getKey(), idx);
            }
            return result;
        }
    }

    public static Loader loader() { return new Loader(); }

    public InputMapper() { super(InputType.class); }
    public InputMapper(EnumMap<InputType, ? extends Integer> m) { super(m); }
    public InputMapper(Map<InputType, ? extends Integer> m) { super(m); }

    public void run(ItemTreeEvent.InputEvent event, Runnable... handlers) {
        Integer idx = get(event.input());
        if (idx != null)
            handlers[idx].run();
    }
}
