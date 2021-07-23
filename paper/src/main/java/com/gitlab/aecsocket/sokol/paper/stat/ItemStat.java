package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A stat type which stores an {@link ItemDescriptor}.
 */
public final class ItemStat extends BasicStat<ItemDescriptor> {
    private ItemStat(@Nullable ItemDescriptor def, boolean required) {
        super(new TypeToken<ItemDescriptor>() {}, def, required, (a, b) -> b, i -> i);
    }

    public static ItemStat itemStat(@Nullable ItemDescriptor def) { return new ItemStat(def, false); }
    public static ItemStat itemStat() { return new ItemStat(null, true); }
}
