package com.gitlab.aecsocket.sokol.core.stat;

import java.util.Optional;

public interface Stat<T> {
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

        public Stat<T> stat() { return stat; }
        public T raw() { return value; }

        public T value() { return value == null ? stat.defaultValue() : value; }
        public T value(T value) {
            T old = this.value;
            this.value = value;
            return old;
        }

        public void combineFrom(Instance<T> o) {
            value = stat.combine(value, o.value);
        }

        public Instance<T> copy() { return new Instance<>(this); }

        @Override public String toString() { return "{%s}={%s}".formatted(stat, value); }
    }

    Class<T> type();
    T defaultValue();
    T copy(T v);
    T combine(T a, T b);

    default Instance<T> instance(T value) {
        return new Instance<>(this, value);
    }
}
