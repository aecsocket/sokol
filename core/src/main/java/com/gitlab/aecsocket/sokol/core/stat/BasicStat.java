package com.gitlab.aecsocket.sokol.core.stat;

import io.leangen.geantyref.TypeToken;

import java.util.function.Function;

/**
 * An abstract base stat type, supporting operations.
 * <p>
 * An implementation must provide, through a {@code #super} constructor call:
 * <ul>
 *     <li>a {@link TypeToken}</li>
 *     <li>a default {@link Operator}</li>
 * </ul>
 * An implementation should, if possible, also provide:
 * <ul>
 *     <li>all {@link Operator}s as public static fields, in the format {@code OP_[name of operator]}</li>
 *     <li>a public static field of the default operator, in the name {@code OP_DEF}</li>
 *     <li>a publicly accessible {@link Operators} instance, with all the defined operators and defined operator specified</li>
 *     <li>an implementation of {@link Descriptor.Serializer} for the type of the stat</li>
 * </ul>
 * A basic example implementation can be found in {@link com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat}.
 */
public abstract class BasicStat<T> implements Stat<Descriptor<T>> {

    public static <T> Operator<T> op(String key, Function<OperatorContext<T>, T> function, TypeToken<?>... args) {
        return new Operator<>(key, args, function);
    }

    public static <T> Operator<T> op(String key, Function<OperatorContext<T>, T> function, Class<?>... args) {
        TypeToken<?>[] argTypes = new TypeToken[args.length];
        for (int i = 0; i < args.length; i++)
            argTypes[i] = TypeToken.get(args[i]);
        return new Operator<>(key, argTypes, function);
    }

    private final TypeToken<Descriptor<T>> type;
    private final Operator<T> defaultOperator;

    public BasicStat(TypeToken<Descriptor<T>> type, Operator<T> defaultOperator) {
        this.type = type;
        this.defaultOperator = defaultOperator;
    }

    public Descriptor<T> desc(T val) {
        return Descriptor.single(defaultOperator, val);
    }

    @Override public TypeToken<Descriptor<T>> type() { return type; }
    @Override public Descriptor<T> combine(Descriptor<T> a, Descriptor<T> b) { return desc(b.combine(a.combine(null))); }
    @Override public Descriptor<T> copy(Descriptor<T> v) { return v.copy(); }
}
