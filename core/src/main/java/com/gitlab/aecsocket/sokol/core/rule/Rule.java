package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.node.RuleException;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.*;

public interface Rule {
    void applies(Node node) throws RuleException;
    void visit(Visitor visitor);

    interface Visitor {
        void visit(Rule rule);
    }

    final class Constant implements Rule {
        public static final Constant TRUE = new Constant(true);
        public static final Constant FALSE = new Constant(false);

        private final boolean value;

        private Constant(boolean value) {
            this.value = value;
        }

        public boolean value() { return value; }

        public static Constant of(boolean value) {
            return value ? TRUE : FALSE;
        }

        @Override
        public void applies(Node node) throws RuleException {
            if (!value)
                throw new RuleException(this);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Constant constant = (Constant) o;
            return value == constant.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    final class Serializer implements TypeSerializer<Rule> {
        private @Nullable Map<String, Class<? extends Rule>> types;

        public @Nullable Map<String, Class<? extends Rule>> types() { return types; }
        public void types(@Nullable Map<String, Class<? extends Rule>> types) { this.types = types; }

        @Override
        public void serialize(Type type, @Nullable Rule obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        private <T, C extends Collection<T>> C map(Class<T> type, C collection, List<? extends ConfigurationNode> nodes) throws SerializationException {
            for (var node : nodes)
                collection.add(require(node, type));
            return collection;
        }

        @Override
        public Rule deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (types == null)
                throw new SerializationException(node, type, "No types provided");

            if (node.raw() instanceof Boolean value)
                return Constant.of(value);
            if (node.isList()) {
                List<? extends ConfigurationNode> nodes = new ArrayList<>(node.childrenList());
                if (nodes.size() == 0)
                    throw new SerializationException(node, type, "No operator specified");
                String op = require(nodes.get(0), String.class);
                nodes.remove(0);
                int terms = nodes.size();
                return switch (op) {
                    case "!" -> {
                        if (terms != 1)
                            throw new SerializationException(node, type, "Operator NOT requires [term], found " + terms);
                        yield new LogicRule.Not(require(nodes.get(0), Rule.class));
                    }
                    case "&" -> new LogicRule.And(map(Rule.class, new ArrayList<>(), nodes));
                    case "|" -> new LogicRule.Or(map(Rule.class, new ArrayList<>(), nodes));

                    case "?" -> {
                        if (terms != 1)
                            throw new SerializationException(node, type, "Operator HAS requires [path], found " + terms);
                        yield new NavigationRule.Has(require(nodes.get(0), NodePath.class));
                    }
                    case "$" -> {
                        if (terms != 2)
                            throw new SerializationException(node, type, "Operator AS requires [path, term], found " + terms);
                        yield new NavigationRule.As(
                                require(nodes.get(0), NodePath.class),
                                require(nodes.get(1), Rule.class)
                        );
                    }
                    case "/" -> {
                        if (terms != 1)
                            throw new SerializationException(node, type, "Operator AS_ROOT requires [path, term], found " + terms);
                        yield new NavigationRule.AsRoot(
                                require(nodes.get(0), NodePath.class),
                                require(nodes.get(1), Rule.class)
                        );
                    }
                    case "/?" -> NavigationRule.IsRoot.INSTANCE;

                    case "#" -> new ComponentRule.HasTags(map(String.class, new HashSet<>(), nodes));
                    case "~" -> new ComponentRule.HasFeatures(map(String.class, new HashSet<>(), nodes));
                    default -> throw new SerializationException(node, type, "Invalid operator [" + op + "]");
                };
            }

            String typeName = require(node.isMap() ? node.node("type") : node, String.class);
            Class<? extends Rule> typeClass = types.get(typeName);
            if (typeClass == null)
                throw new SerializationException(node, type, "Invalid rule type [" + typeName + "], accepts: [" +
                        String.join(", ", types.keySet()) + "]");
            return (node.isMap() ? node : BasicConfigurationNode.root(node.options())).get(typeClass);
        }
    }
}
