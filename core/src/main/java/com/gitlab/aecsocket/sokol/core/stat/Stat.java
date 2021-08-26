package com.gitlab.aecsocket.sokol.core.stat;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a type of value, dictating behaviour for an {@link Node}.
 * @param <T> The stored value type.
 */
public abstract class Stat<T> {
    /**
     * A node in a linked list of stat instances, evaluating to a value.
     * @param <T> The stored value type.
     */
    public static final class Node<T> {
        private final Stat<T> stat;
        private final Operator<T> operator;
        private final List<Object> args;
        private @Nullable Node<T> next;

        public Node(Stat<T> stat, Operator<T> operator, List<Object> args, @Nullable Node<T> next) {
            this.stat = stat;
            this.operator = operator;
            this.args = args;
            this.next = next;
        }

        public Node(Stat<T> stat, Operator<T> operator, List<Object> args) {
            this.stat = stat;
            this.operator = operator;
            this.args = args;
        }

        /**
         * Gets the underlying stat.
         * @return The stat.
         */
        public Stat<T> stat() { return stat; }

        public Operator<T> operator() { return operator; }

        public List<Object> args() { return args; }

        /**
         * Gets the next node in this linked list.
         * @return The node.
         */
        public @Nullable Node<T> next() { return next; }

        /**
         * Sets the next node in this linked list.
         * @param next The node.
         */
        public void chain(Node<T> next) { this.next = next; }

        public T operate(@Nullable T base) {
            return operator.operate(base, args);
        }

        public Optional<T> value() {
            T value = null;
            Node<T> node = this;
            while ((node = node.next) != null) {
                value = node.operate(value);
            }
            return Optional.ofNullable(value);
        }

        public Node<T> copy() {
            return new Node<>(stat, operator, new ArrayList<>(args), next == null ? null : next.copy());
        }

        @Override
        public String toString() {
            return operator.key() + args + (next == null ? "" : " " + next);
        }
    }

    private final String key;

    public Stat(String key) {
        this.key = key;
    }

    /**
     * Gets the key of this stat.
     * @return The key.
     */
    public String key() { return key; }

    /**
     * Gets the operators that this stat supports.
     * @return The operators.
     */
    public abstract Map<String, Operator<T>> operators();

    /**
     * Gets the default operator of this stat.
     * @return The default operator.
     */
    public abstract Operator<T> defaultOperator();
}
