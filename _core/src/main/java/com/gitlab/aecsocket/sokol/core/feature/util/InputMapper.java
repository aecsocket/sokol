package com.gitlab.aecsocket.sokol.core.feature.util;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.feature.Feature;
import com.gitlab.aecsocket.sokol.core.util.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.PlayerUser;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ConfigSerializable
public class InputMapper extends EnumMap<InputType, List<InputMapper.Entry>> {
    public static final class Serializer implements TypeSerializer<InputMapper> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Type type, @Nullable InputMapper obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.set(new TypeToken<Map<InputType, List<InputMapper.Entry>>>() {}, obj);
            }
        }

        @Override
        public InputMapper deserialize(Type type, ConfigurationNode node) throws SerializationException {
            var map = Serializers.require(node, new TypeToken<Map<InputType, List<Entry>>>() {});
            return map.size() == 0 ? new InputMapper() : new InputMapper(map);
        }
    }

    public enum Condition {
        SNEAKING    (PlayerUser::sneaking),
        SPRINTING   (PlayerUser::sprinting);

        private final Predicate<PlayerUser> test;

        Condition(Predicate<PlayerUser> test) {
            this.test = test;
        }

        public boolean test(PlayerUser player) {
            return test.test(player);
        }
    }

    @ConfigSerializable
    public record Entry(Map<Condition, Boolean> conditions, Rule rule, @Required Set<String> actions, boolean stop) {}

    public InputMapper() { super(InputType.class); }
    public InputMapper(EnumMap<InputType, ? extends List<InputMapper.Entry>> m) { super(m); }
    public InputMapper(Map<InputType, ? extends List<InputMapper.Entry>> m) { super(m); }

    public void run(Feature.Instance system, ItemTreeEvent.Input event, Consumer<CollectionBuilder.OfMap<String, Runnable>> handlers) {
        var handlersBuilder = CollectionBuilder.map(new HashMap<String, Runnable>());
        handlers.accept(handlersBuilder);
        Map<String, Runnable> builtHandlers = handlersBuilder.get();

        if (!(event.user() instanceof PlayerUser player))
            return;
        AllEntries:
        for (var entry : getOrDefault(event.input(), Collections.emptyList())) {
            if (entry.conditions != null) {
                for (var condition : entry.conditions.entrySet()) {
                    if (condition.getKey().test(player) != condition.getValue()) {
                        continue AllEntries;
                    }
                }
            }
            if (entry.rule != null && !entry.rule.applies(system.parent()))
                continue;

            for (var action : entry.actions) {
                Runnable runnable = builtHandlers.get(action);
                if (runnable == null)
                    throw new IllegalArgumentException("Invalid action [" + action + "]");
                runnable.run();

                if (entry.stop)
                    break AllEntries;
            }
        }
    }
}
