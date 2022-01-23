package old.stat;

import com.github.aecsocket.minecommons.core.translation.Localizer;
import com.github.aecsocket.sokol.core.stat.ListStat;
import com.gitlab.aecsocket.minecommons.core.effect.SoundEffect;

import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;

public final class SoundsStat extends ListStat<SoundEffect> {
    private static final Function<? super SoundEffect, Component> renderer = e -> text(""+e, CONSTANT);

    public record SetValue(List<SoundEffect> value) implements ListStat.SetValue<SoundEffect> {
        @Override public String toString() { return asString(Locale.ROOT); }
        @Override public Component render(Locale locale, Localizer lc) { return ListStat.render(value, renderer); }
    }

    public record AddValue(List<SoundEffect> value) implements ListStat.SetValue<SoundEffect> {
        @Override public String toString() { return asString(Locale.ROOT); }
        @Override public Component render(Locale locale, Localizer lc) { return text(operator(), CONSTANT).append(ListStat.render(value, renderer)); }
    }

    private static final OperationDeserializer<List<SoundEffect>> opDeserializer = OperationDeserializer.<List<SoundEffect>>builder()
            .operation("=", (type, node, args) -> new SetValue(require(args[0], new TypeToken<List<SoundEffect>>() {})), "value")
            .operation("+", (type, node, args) -> new AddValue(require(args[0], new TypeToken<List<SoundEffect>>() {})), "value")
            .defaultOperation("=")
            .build();

    private SoundsStat(String key, @Nullable List<SoundEffect> defaultValue) {
        super(key, defaultValue);
    }

    public static SoundsStat soundsStat(String key) { return new SoundsStat(key, null); }
    public static SoundsStat soundsStat(String key, List<SoundEffect> def) { return new SoundsStat(key, def); }

    @Override
    public Value<List<SoundEffect>> deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return opDeserializer.deserialize(type, node);
    }

    @Override
    public Component renderValue(Locale locale, Localizer lc, List<SoundEffect> value) {
        return render(value, renderer);
    }
}
