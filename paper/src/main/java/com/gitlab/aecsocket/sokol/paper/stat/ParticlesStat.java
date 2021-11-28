package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.minecommons.paper.effect.ParticleEffect;
import com.gitlab.aecsocket.sokol.core.stat.ListStat;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.*;
import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;

public final class ParticlesStat extends ListStat<ParticleEffect> {
    private static final Function<? super ParticleEffect, Component> renderer = e -> text(""+e, CONSTANT);

    public record SetValue(List<ParticleEffect> value) implements ListStat.SetValue<ParticleEffect> {
        @Override public String toString() { return asString(Locale.ROOT); }
        @Override public Component render(Locale locale, Localizer lc) { return ListStat.render(value, renderer); }
    }

    public record AddValue(List<ParticleEffect> value) implements ListStat.SetValue<ParticleEffect> {
        @Override public String toString() { return asString(Locale.ROOT); }
        @Override public Component render(Locale locale, Localizer lc) { return text(operator(), CONSTANT).append(ListStat.render(value, renderer)); }
    }

    private static final OperationDeserializer<List<ParticleEffect>> opDeserializer = OperationDeserializer.<List<ParticleEffect>>builder()
            .operation("=", (type, node, args) -> new SetValue(require(args[0], new TypeToken<List<ParticleEffect>>() {})), "value")
            .operation("+", (type, node, args) -> new AddValue(require(args[0], new TypeToken<List<ParticleEffect>>() {})), "value")
            .defaultOperation("=")
            .build();

    private ParticlesStat(String key, @Nullable List<ParticleEffect> defaultValue) {
        super(key, defaultValue);
    }

    public static ParticlesStat particlesStat(String key) { return new ParticlesStat(key, null); }
    public static ParticlesStat particlesStat(String key, List<ParticleEffect> def) { return new ParticlesStat(key, def); }

    @Override
    public Value<List<ParticleEffect>> deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return opDeserializer.deserialize(type, node);
    }

    @Override
    public Component renderValue(Locale locale, Localizer lc, List<ParticleEffect> value) {
        return render(value, renderer);
    }
}
