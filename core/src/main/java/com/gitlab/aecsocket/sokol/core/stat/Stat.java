package com.gitlab.aecsocket.sokol.core.stat;

import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a type of value, dictating behaviour for an {@link Instance}.
 * @param <T> The stored value type.
 */
public interface Stat<T> extends Copier<T>, Combiner<T> {
    /**
     * An instance of a stat, storing a value.
     * @param <T> The stored value type.
     */
    final class Instance<T> {
        private final Stat<T> stat;
        private T value;

        public Instance(Stat<T> stat, T value) {
            this.stat = stat;
            this.value = value;
        }

        public Instance(Instance<T> o) {
            stat = o.stat;
            value = stat.copy(o.value);
        }

        /**
         * Gets the underlying stat.
         * @return The stat.
         */
        public Stat<T> stat() { return stat; }

        /**
         * Gets the raw value.
         * @return The value.
         */
        public T raw() { return value; }

        /**
         * Gets the value if it is present, otherwise {@link Stat#defaultValue()}.
         * @return The value.
         */
        public T value() { return value == null ? stat.defaultValue() : value; }

        /**
         * Sets the value of this instance.
         * @param value The new value.
         */
        public void value(@Nullable T value) { this.value = value; }

        /**
         * Combines this instance's value from another instance's value, using {@link Stat#combine(Object, Object)}.
         * @param o The other instance.
         */
        public void combineFrom(Instance<T> o) {
            value = stat.combine(value, o.value);
        }

        /**
         * Copies this instance and the value inside, using {@link Stat#copy(Object)}.
         * @return The copy.
         */
        public Instance<T> copy() { return new Instance<>(this); }

        @Override public String toString() { return "{%s}={%s}".formatted(stat, value); }
    }

    /**
     * Gets the type of value deserialized.
     * @return The type.
     */
    TypeToken<T> type();

    /**
     * Gets the default value to be used, in the absence of another value.
     * @return The value.
     */
    T defaultValue();

    /**
     * Creates an instance from this stat.
     * @param value The value of the instance.
     * @return The instance.
     */
    default Instance<T> instance(T value) {
        return new Instance<>(this, value);
    }
}
