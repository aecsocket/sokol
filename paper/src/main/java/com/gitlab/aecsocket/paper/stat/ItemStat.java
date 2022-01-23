package com.gitlab.aecsocket.paper.stat;

import com.github.aecsocket.sokol.core.stat.AbstractStat;
import com.gitlab.aecsocket.paper.ItemDescriptor;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Locale;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;

public final class ItemStat extends AbstractStat<ItemDescriptor> {
    public record SetValue(ItemDescriptor value) implements InitialValue<ItemDescriptor> {
        @Override public ItemDescriptor compute(ItemDescriptor cur) { return value; }
        @Override public boolean discardsPrevious() { return true; }
        @Override public ItemDescriptor first() { return value; }
        @Override public String toString() { return ""+value; }
        @Override public Component render(I18N i18n, Locale locale) { return text(value.toString(), CONSTANT); }
    }

    private static final OperationDeserializer<ItemDescriptor> opDeserializer = OperationDeserializer.<ItemDescriptor>builder()
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

    @Override
    public Component renderValue(Locale locale, Localizer lc, ItemDescriptor value) {
        return text(value.toString(), CONSTANT);
    }

    public SetValue set(ItemDescriptor value) { return new SetValue(value); }
}
