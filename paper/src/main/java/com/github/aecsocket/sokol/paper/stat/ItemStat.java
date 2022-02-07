package com.github.aecsocket.sokol.paper.stat;

import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.paper.ItemDescriptor;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;

public final class ItemStat extends Stat<ItemDescriptor> {
    public record Set(ItemDescriptor value) implements Stat.Op.Initial<ItemDescriptor>, Stat.Op.Discards {
        @Override public ItemDescriptor first() { return value; }
    }

    private static final OpTypes<ItemDescriptor> OP_TYPES = Stat.<ItemDescriptor>buildOpTypes()
        .setDefault("=", (type, node, args) -> new Set(require(args[0], ItemDescriptor.class)), "item")
        .build();

    private ItemStat(String key, @Nullable ItemDescriptor def) {
        super(key, def);
    }

    public static ItemStat itemStat(String key, ItemDescriptor def) {
        return new ItemStat(key, def);
    }

    public static ItemStat itemStat(String key) {
        return new ItemStat(key, null);
    }

    @Override public OpTypes<ItemDescriptor> opTypes() { return OP_TYPES; }
}
