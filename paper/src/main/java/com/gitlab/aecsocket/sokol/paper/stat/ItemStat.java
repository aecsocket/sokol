package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.stat.AbstractStat;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.paper.ItemDescriptor;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Locale;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;
import static net.kyori.adventure.text.Component.text;

public final class ItemStat extends AbstractStat<ItemDescriptor> {
    public record SetValue(ItemDescriptor value) implements InitialValue<ItemDescriptor> {
        @Override public ItemDescriptor compute(ItemDescriptor cur) { return value; }
        @Override public ItemDescriptor first() { return value; }
        @Override public Component render(Locale locale, Localizer lc) { return text("=", OPERATOR).append(text(value.toString(), CONSTANT)); }
    }

    private static final Stat.OperationDeserializer<ItemDescriptor> opDeserializer = Stat.OperationDeserializer.<ItemDescriptor>builder()
            .operation("=", (type, node, args) -> new SetValue(require(args[0], ItemDescriptor.class)), "value")
            .defaultOperation("=")
            .build();

    private ItemStat(String key, @Nullable ItemDescriptor defaultValue) {
        super(key, defaultValue);
    }

    public static ItemStat itemStat(String key) { return new ItemStat(key, null); }
    public static ItemStat itemStat(String key, ItemDescriptor def) { return new ItemStat(key, def); }

    @Override
    public Value<ItemDescriptor> deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return opDeserializer.deserialize(type, node);
    }

    public SetValue set(ItemDescriptor value) { return new SetValue(value); }
}
