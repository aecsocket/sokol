package com.github.aecsocket.sokol.core.rule.base;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Map;

import com.github.aecsocket.sokol.core.api.BaseNode;

public interface BaseRule {
    <N extends BaseNode.Scoped<N, ?>> void applies(N target, N parent) throws BaseRuleException;

    abstract class Serializer<R extends BaseRule> implements TypeSerializer<R> {
        public static final String TYPE = "type";
        public static final String RULE = "rule";
        public static final String KEY = "key";
        public static final String ARGS = "args";

        protected abstract Map<String, Class<? extends R>> types();

        @Override
        public void serialize(Type type, @Nullable R obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public R deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (node.isMap()) {
                if (node.hasChild(TYPE)) {

                } else if (node.hasChild(RULE)) {

                } else
                    throw new SerializationException(node, type, "Invalid rule format: if map, must either be annotation or typed");
            } else if (node.isList()) {
                // todo operators
            } else {

            }
        }
    }
}
