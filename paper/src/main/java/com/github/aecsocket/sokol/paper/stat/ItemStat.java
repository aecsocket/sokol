package com.github.aecsocket.sokol.paper.stat;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.paper.ItemDescriptor;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.stream.Stream;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;
import static net.kyori.adventure.text.Component.*;

public final class ItemStat extends Stat<ItemDescriptor> {
    public record Set(ItemDescriptor value) implements Stat.Op.Initial<ItemDescriptor>, Stat.Op.Discards {
        @Override public ItemDescriptor first() { return value; }
        @Override
        public Component render(I18N i18n, Locale locale) {
            return renderDescriptor(i18n, locale, value);
        }
    }
    public record Modify(int modelData, int damage) implements Stat.Op<ItemDescriptor> {
        @Override
        public ItemDescriptor compute(ItemDescriptor cur) {
            return new ItemDescriptor(
                cur.key(), cur.modelData() + modelData, cur.damage() + damage, cur.unbreakable(), cur.flags()
            );
        }

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, STAT_TYPE_ITEM_MODIFY,
                c -> c.of("model_data", () -> text(modelData)),
                c -> c.of("damage", () -> text(damage)));
        }
    }

    public static final String
        STAT_TYPE_ITEM_SET = "stat_type.item.set",
        STAT_TYPE_ITEM_MODIFY = "stat_type.item.modify";
    private static final OpTypes<ItemDescriptor> OP_TYPES = Stat.<ItemDescriptor>buildOpTypes()
        .setDefault("=", (type, node, args) -> new Set(require(args[0], ItemDescriptor.class)), "item")
        .set("+", (type, node, args) -> new Modify(require(args[0], int.class), require(args[1], int.class)), "model_data", "damage")
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

    @Override
    public Component renderValue(I18N i18n, Locale locale, ItemDescriptor value) {
        return renderDescriptor(i18n, locale, value);
    }

    public static Component renderDescriptor(I18N i18n, Locale locale, ItemDescriptor value) {
        return i18n.line(locale, STAT_TYPE_ITEM_SET,
            c -> c.of("key", () -> text(""+value.key())),
            c -> c.of("model_data", () -> text(value.modelData())),
            c -> c.of("damage", () -> text(value.damage())),
            c -> c.of("unbreakable", () -> value.unbreakable() ? c.line(SokolPlatform.YES) : c.line(SokolPlatform.NO)),
            c -> c.of("flags", () -> text(String.join(", ", Stream.of(value.flags()).map(Enum::name).toList()))));
    }
}
