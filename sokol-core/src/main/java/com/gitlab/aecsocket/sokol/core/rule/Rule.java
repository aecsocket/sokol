package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;

/**
 * An expression which can be evaluated on a {@link TreeNode}.
 */
public interface Rule {
    /**
     * The default rule types, mapped to their string type.
     */
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
            .put(ComponentRule.HasTags.TYPE, ComponentRule.HasTags.class)
            .put(ComponentRule.HasSystems.TYPE, ComponentRule.HasSystems.class)
            .build();

    /**
     * A serializer for rules.
     * <p>
     * If a node being evaluated is a list, uses the shortcuts:
     * <ul>
     *     <li>{@code !}: {@link LogicRule.Not}</li>
     *     <li>{@code &}: {@link LogicRule.And}</li>
     *     <li>{@code |}: {@link LogicRule.Or}</li>
     *
     *     <li>{@code ?}: {@link NavigationRule.Has}</li>
     *     <li>{@code $}: {@link NavigationRule.As}</li>
     *     <li>{@code /}: {@link NavigationRule.AsRoot}</li>
     *     <li>{@code ?}: {@link NavigationRule.IsRoot}</li>
     *
     *     <li>{@code #}: {@link ComponentRule.HasTags}</li>
     * </ul>
     */
    final class Serializer implements TypeSerializer<Rule> {
        private Map<String, Class<? extends Rule>> types;

        public Map<String, Class<? extends Rule>> types() { return types; }
        public void types(Map<String, Class<? extends Rule>> types) { this.types = types; }

        @Override
        public void serialize(Type type, @Nullable Rule obj, ConfigurationNode node) throws SerializationException {}

        private <T> List<T> terms(Class<T> type, List<? extends ConfigurationNode> children, int start) throws SerializationException {
            List<T> terms = new ArrayList<>();
            for (int i = start; i < children.size(); i++) {
                terms.add(require(children.get(i), type));
            }
            return terms;
        }

        @Override
        public Rule deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (types == null)
                throw new SerializationException(node, type, "No types provided");

            if (node.raw() instanceof Boolean value)
                // Primitive
                return Constant.of(value);

            if (node.isList()) {
                // Operator
                List<? extends ConfigurationNode> children = node.childrenList();
                if (children.size() < 2)
                    throw new SerializationException(node, type, "Operator rules require at least an operator and operand");

                String operator = require(children.get(0), String.class);
                return switch (operator) {
                    case "!" -> {
                        if (children.size() != 2)
                            throw new SerializationException(node, type, "Operator NOT requires [NOT, term], gave " + children.size());
                        yield new LogicRule.Not(require(children.get(1), Rule.class));
                    }
                    case "&" -> new LogicRule.And(terms(Rule.class, children, 1));
                    case "|" -> new LogicRule.Or(terms(Rule.class, children, 1));

                    case "?" -> {
                        if (children.size() != 2)
                            throw new SerializationException(node, type, "Operator HAS requires [HAS, path], gave " + children.size());
                        yield new NavigationRule.Has(require(children.get(1), String[].class));
                    }
                    case "$" -> {
                        if (children.size() != 3)
                            throw new SerializationException(node, type, "Operator AS requires [AS, path, term], gave " + children.size());
                        yield new NavigationRule.As(
                                require(children.get(1), String[].class),
                                require(children.get(2), Rule.class)
                        );
                    }
                    case "/" -> {
                        if (children.size() != 3)
                            throw new SerializationException(node, type, "Operator AS_ROOT requires [AS_ROOT, path, term], gave " + children.size());
                        yield new NavigationRule.AsRoot(
                                require(children.get(1), String[].class),
                                require(children.get(2), Rule.class)
                        );
                    }
                    case "/?" -> NavigationRule.IsRoot.INSTANCE;

                    case "#" -> new ComponentRule.HasTags(new HashSet<>(terms(String.class, children, 1)));
                    default -> throw new SerializationException(node, type, "Invalid logical operator [" + operator + "]");
                };
            }

            String typeName = require(node.node("type"), String.class);
            Class<? extends Rule> typeClass = types.get(typeName);
            if (typeClass == null)
                throw new SerializationException(node, type, "Invalid rule type [" + typeName + "]");
            return node.get(typeClass);
        }
    }

    /**
     * A rule which returns a constant value.
     */
    @ConfigSerializable
    final class Constant implements Rule {
        /** The rule type. */
        public static final String TYPE = "constant";
        /** A singleton instance for the expression of {@code true}. */
        public static final Constant TRUE = new Constant(true);
        /** A singleton instance for the expression of {@code false}. */
        public static final Constant FALSE = new Constant(false);

        @Required private final boolean value;

        public Constant(boolean value) {
            this.value = value;
        }

        private Constant() { this(false); }

        /**
         * Gets either {@link #TRUE} or {@link #FALSE} depending on the value passed.
         * @param value The value.
         * @return The result.
         */
        public static @NotNull Constant of(boolean value) {
            return value ? TRUE : FALSE;
        }

        @Override public @NotNull String type() { return TYPE; }

        public boolean value() { return value; }

        @Override
        public boolean applies(@NotNull TreeNode node) {
            return value;
        }

        @Override
        public void visit(@NotNull Visitor visitor) {
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

    /**
     * Gets the type name of this rule, used in deserialization.
     * @return The type.
     */
    @NotNull String type();

    /**
     * Evaluates this rule on a node.
     * @param node The node.
     * @return The result.
     */
    boolean applies(@NotNull TreeNode node);

    /**
     * Recursively applies a visitor function to this node and its children.
     * @param visitor The visitor function.
     */
    void visit(@NotNull Visitor visitor);
}
