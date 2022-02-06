package com.github.aecsocket.sokol.paper.stat;

import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.paper.PaperItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;

public final class ItemStat extends Stat<PaperItemStack> {
    public record Set(PaperItemStack value) implements Stat.Op.Initial<PaperItemStack>, Stat.Op.Discards {
        @Override public PaperItemStack first() { return value; }
    }

    private static final OpTypes<PaperItemStack> OP_TYPES = Stat.<PaperItemStack>buildOpTypes()
        .setDefault("=", (type, node, args) -> new Set(require(args[0], PaperItemStack.class)), "item")
        .build();

    private ItemStat(String key, @Nullable PaperItemStack def) {
        super(key, def);
    }

    public static ItemStat itemStat(String key, PaperItemStack def) {
        return new ItemStat(key, def);
    }

    public static ItemStat itemStat(String key) {
        return new ItemStat(key, null);
    }

    @Override public OpTypes<PaperItemStack> opTypes() { return OP_TYPES; }
}
