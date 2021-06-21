package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public interface Rule {
    Map<String, Class<? extends Rule>> BASE_RULE_TYPES = CollectionBuilder.map(new HashMap<String, Class<? extends Rule>>())
            .put(Constant.TYPE, Constant.class)

            .put(LogicRule.Not.TYPE, LogicRule.Not.class)
            .put(LogicRule.And.TYPE, LogicRule.And.class)
            .put(LogicRule.Or.TYPE, LogicRule.Or.class)

            .put(NavigationRule.Has.TYPE, NavigationRule.Has.class)
            .put(NavigationRule.As.TYPE, NavigationRule.As.class)
            .put(NavigationRule.AsRoot.TYPE, NavigationRule.AsRoot.class)
            .put(NavigationRule.IsRoot.TYPE, NavigationRule.IsRoot.class)
            .put(NavigationRule.AsChild.TYPE, NavigationRule.AsChild.class)
            .put(NavigationRule.AsParent.TYPE, NavigationRule.AsParent.class)

            .put(ComponentRule.Complete.TYPE, ComponentRule.Complete.class)
            .put(ComponentRule.HasTag.TYPE, ComponentRule.HasTag.class)
            .put(ComponentRule.HasSystem.TYPE, ComponentRule.HasSystem.class)
            .build();

    final class Serializer implements TypeSerializer<Rule> {
        private Map<String, Class<? extends Rule>> ruleTypes;

        public Map<String, Class<? extends Rule>> ruleTypes() { return ruleTypes; }
        public void ruleTypes(Map<String, Class<? extends Rule>> ruleTypes) { this.ruleTypes = ruleTypes; }

        @Override
        public void serialize(Type type, @Nullable Rule obj, ConfigurationNode node) throws SerializationException {}

        private List<Rule> terms(List<? extends ConfigurationNode> children, int start) throws SerializationException {
            List<Rule> terms = new ArrayList<>();
            for (int i = start; i < children.size(); i++) {
                terms.add(children.get(i).require(Rule.class));
            }
            return terms;
        }

        @Override
        public Rule deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (ruleTypes == null)
                throw new SerializationException(node, type, "No base map provided");

            if (node.raw() instanceof Boolean value)
                // Primitive
                return Constant.of(value);

            if (node.isList()) {
                // Operator
                List<? extends ConfigurationNode> children = node.childrenList();
                if (children.size() < 2)
                    throw new SerializationException(node, type, "Operator rules require at least an operator and operand");

                String operator = children.get(0).require(String.class);
                return switch (operator) {
                    case "!" -> {
                        if (children.size() != 2)
                            throw new SerializationException(node, type, "Operator NOT requires [NOT, term], gave " + children.size());
                        yield new LogicRule.Not(children.get(1).require(Rule.class));
                    }
                    case "&" -> new LogicRule.And(terms(children, 1));
                    case "|" -> new LogicRule.Or(terms(children, 1));

                    case "#" -> {
                        if (children.size() != 2)
                            throw new SerializationException(node, type, "Operator HAS requires [HAS, path], gave " + children.size());
                        yield new NavigationRule.Has(children.get(1).require(String[].class));
                    }
                    case "$" -> {
                        if (children.size() != 3)
                            throw new SerializationException(node, type, "Operator AS requires [AS, path, term], gave " + children.size());
                        yield new NavigationRule.As(
                                children.get(1).require(String[].class),
                                children.get(2).require(Rule.class)
                        );
                    }
                    case "/" -> {
                        if (children.size() != 3)
                            throw new SerializationException(node, type, "Operator AS_ROOT requires [AS_ROOT, path, term], gave " + children.size());
                        yield new NavigationRule.AsRoot(
                                children.get(1).require(String[].class),
                                children.get(2).require(Rule.class)
                        );
                    }
                    case "/?" -> NavigationRule.IsRoot.INSTANCE;
                    default -> throw new SerializationException(node, type, "Invalid logical operator [" + operator + "]");
                };
            }

            String typeName = node.node("type").require(String.class);
            Class<? extends Rule> typeClass = ruleTypes.get(typeName);
            if (typeClass == null)
                throw new SerializationException(node, type, "Invalid rule type [" + typeName + "]");
            return node.get(typeClass);
        }
    }

    @ConfigSerializable
    final class Constant implements Rule {
        public static final String TYPE = "constant";
        public static final Constant TRUE = new Constant(true);
        public static final Constant FALSE = new Constant(false);

        @Required private final boolean value;

        public Constant(boolean value) {
            this.value = value;
        }

        private Constant() { this(false); }

        public static Constant of(boolean value) {
            return value ? TRUE : FALSE;
        }

        @Override public String type() { return TYPE; }

        public boolean value() { return value; }

        @Override
        public boolean applies(TreeNode tree) {
            return value;
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

        @Override
        public String toString() { return "<" + value + ">"; }
    }

    String type();
    boolean applies(TreeNode tree);
    void visit(Visitor visitor);
}
