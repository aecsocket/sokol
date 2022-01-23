package com.github.aecsocket.sokol.core.stat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.aecsocket.minecommons.core.translation.Localizer;
import com.github.aecsocket.sokol.core.Renderable;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;

public abstract class ListStat<E> extends AbstractStat<List<E>> {
    public static final Component LEFT_BRACKET = text("[", Renderable.SEPARATOR);
    public static final JoinConfiguration SEPARATOR = separator(text(", ", Renderable.SEPARATOR));
    public static final Component RIGHT_BRACKET = text("]", Renderable.SEPARATOR);

    public static <E> Component render(List<E> list, Function<? super E, Component> renderer) {
        return LEFT_BRACKET
                .append(join(SEPARATOR, list.stream()
                        .map(renderer)
                        .collect(Collectors.toList())))
                .append(RIGHT_BRACKET);
    }

    public interface BaseValue<E> extends InitialValue<List<E>> {
        List<E> value();
        String operator();

        @Override default boolean discardsPrevious() { return false; }
        @Override default List<E> first() { return value(); }
        default String asString(Locale locale) { return operator() + value(); }
    }

    public interface SetValue<E> extends BaseValue<E> {
        @Override default String operator() { return "="; }
        @Override default List<E> compute(List<E> cur) { return cur; }
        @Override default boolean discardsPrevious() { return true; }
        @Override default String asString(Locale locale) { return ""+value(); }
    }

    public interface AddValue<E> extends BaseValue<E> {
        @Override default String operator() { return "+"; }
        @Override default List<E> compute(List<E> cur) {
            List<E> result = new ArrayList<>(cur);
            result.addAll(value());
            return result;
        }
    }

    protected ListStat(String key, @Nullable List<E> defaultValue) {
        super(key, defaultValue);
    }
}
