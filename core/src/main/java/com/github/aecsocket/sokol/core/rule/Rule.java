package com.github.aecsocket.sokol.core.rule;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.SokolNode;

import com.github.aecsocket.sokol.core.rule.impl.ComponentRule;
import com.github.aecsocket.sokol.core.rule.impl.LogicRule;
import com.github.aecsocket.sokol.core.rule.impl.NavRule;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.CheckedSupplier;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;

public interface Rule {
    void applies(SokolNode target) throws RuleException;

    default void visit(Consumer<Rule> visitor) {
        visitor.accept(this);
    }

    default Rule withParent(SokolNode node) {
        visit(r -> {
            if (r instanceof AcceptsParent rule) {
                rule.acceptParent(node);
            }
        });
        return this;
    }

    interface AcceptsParent extends Rule {
        void acceptParent(SokolNode node);
    }

    final class Serializer implements TypeSerializer<Rule> {
        public static final String TYPE = "type";

        private @Nullable Map<String, Class<? extends Rule>> types;

        public Map<String, Class<? extends Rule>> types() { return types; }
        public void types(Map<String, Class<? extends Rule>> types) { this.types = types; }

        @Override
        public void serialize(Type type, @Nullable Rule obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        private interface Terms {
            String op();

            <T> T setUp(CheckedSupplier<T, SerializationException> value, String... args) throws SerializationException;

            <T> T next(Class<T> type) throws SerializationException;

            <T> List<T> list(Class<T> type) throws SerializationException;

            <T> Set<T> set(Class<T> type) throws SerializationException;
        }

        private Terms terms(List<? extends ConfigurationNode> nodes, Type type, ConfigurationNode config) throws SerializationException {
            String op = require(nodes.get(0), String.class);
            int argc = nodes.size() - 1;
            return new Terms() {
                int idx = 0;
                String[] args;

                @Override
                public String op() { return op; }

                @Override
                public <T> T setUp(CheckedSupplier<T, SerializationException> value, String... args) throws SerializationException {
                    if (argc != args.length)
                        throw new SerializationException(config, type, "Operator `" + op + "` requires [" + String.join(", ", args) + "], found " + argc);
                    idx = 0;
                    this.args = args;
                    return value.get();
                }

                @Override
                public <T> T next(Class<T> type) throws SerializationException {
                    if (idx + 1 >= nodes.size())
                        throw new SerializationException(config, type, "Unexpected: Reached end of args");
                    T obj;
                    try {
                        obj = require(nodes.get(idx + 1), type);
                    } catch (SerializationException e) {
                        throw new SerializationException(config, type, "Could not deserialize arg " + idx + " of rule");
                    }
                    ++idx;
                    return obj;
                }

                @Override
                public <T> List<T> list(Class<T> type) throws SerializationException {
                    List<T> result = new ArrayList<>();
                    for (int i = 1; i < nodes.size(); i++) {
                        result.add(require(nodes.get(i), type));
                    }
                    return result;
                }

                @Override
                public <T> Set<T> set(Class<T> type) throws SerializationException {
                    Set<T> result = new HashSet<>();
                    for (int i = 1; i < nodes.size(); i++) {
                        result.add(require(nodes.get(i), type));
                    }
                    return result;
                }
            };
        }

        @Override
        public Rule deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (types == null)
                throw new SerializationException(node, type, "No types provided");

            if (node.raw() instanceof Boolean value)
                return LogicRule.Constant.of(value);

            if (node.isList()) {
                var nodes = node.childrenList();
                if (nodes.isEmpty())
                    throw new SerializationException(node, type, "Operator rule must be expressed as [ operator[, ... ] ]");

                String op = require(nodes.get(0), String.class);
                class Terms {
                    final int argc = nodes.size() - 1;
                    int idx = 0;
                    String[] args;

                    public <T> T setUp(CheckedSupplier<T, SerializationException> value, String... args) throws SerializationException {
                        if (argc != args.length)
                            throw new SerializationException(node, type, "Operator `" + op + "` requires [" + String.join(", ", args) + "], found " + argc);
                        idx = 0;
                        this.args = args;
                        return value.get();
                    }

                    public <T> T next(Class<T> type) throws SerializationException {
                        if (idx + 1 >= nodes.size())
                            throw new SerializationException(node, type, "Unexpected: Reached end of args");
                        T obj;
                        try {
                            obj = require(nodes.get(idx + 1), type);
                        } catch (SerializationException e) {
                            throw new SerializationException(node, type, "Could not deserialize arg " + idx + " of rule");
                        }
                        ++idx;
                        return obj;
                    }

                    public <T> List<T> list(Class<T> type) throws SerializationException {
                        List<T> result = new ArrayList<>();
                        for (int i = 1; i < nodes.size(); i++) {
                            result.add(require(nodes.get(i), type));
                        }
                        return result;
                    }

                    public <T> Set<T> set(Class<T> type) throws SerializationException {
                        Set<T> result = new HashSet<>();
                        for (int i = 1; i < nodes.size(); i++) {
                            result.add(require(nodes.get(i), type));
                        }
                        return result;
                    }
                }

                Terms t = new Terms();

                return switch (op) {
                    case "!" -> t.setUp(() -> new LogicRule.Not(t.next(Rule.class)), "term");
                    case "&" -> new LogicRule.And(t.list(Rule.class));
                    case "|" -> new LogicRule.Or(t.list(Rule.class));

                    case "?" -> t.setUp(() -> new NavRule.Has(t.next(NodePath.class)), "path");
                    case "$" -> t.setUp(() -> new NavRule.As(t.next(NodePath.class), t.next(Rule.class)), "path", "term");
                    case "/" -> t.setUp(() -> new NavRule.AsRoot(t.next(NodePath.class), t.next(Rule.class)), "path", "term");
                    case "/?" -> t.setUp(() -> NavRule.IsRoot.INSTANCE);

                    case "#" -> new ComponentRule.HasTags(t.set(String.class));
                    case "~" -> new ComponentRule.HasFeatures(t.set(String.class));
                    case "!?" -> t.setUp(() -> ComponentRule.IsComplete.INSTANCE);
                    default -> throw new SerializationException(node, type, "Invalid operator `" + op + "`");
                };
            } else {
                String ruleType = require(node.isMap() ? node.node(TYPE) : node, String.class);
                Class<? extends Rule> ruleClass = types.get(ruleType);
                if (ruleClass == null)
                    throw new SerializationException(node, type, "Invalid rule type [" + ruleType + "], accepts: [" + String.join(", ", types.keySet()) + "]");
                return (node.isMap() ? node : BasicConfigurationNode.root(node.options())).get(ruleClass);
            }
        }
    }
}
