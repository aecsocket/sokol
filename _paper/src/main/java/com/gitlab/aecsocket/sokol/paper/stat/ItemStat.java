package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;

import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

/**
 * A stat type which stores an {@link ItemDescriptor}.
 */
public final class ItemStat extends Stat<ItemDescriptor> {
    public static final Operator<ItemDescriptor> OP_SET = op("=", c -> c.arg(0), ItemDescriptor.class);
    public static final Operator<ItemDescriptor> OP_MOD = op("mod", c -> {
        ItemDescriptor d = c.reqBase();
        return new ItemDescriptor(d.plugin(), d.material(),
                d.modelData() + (int) c.arg(0),
                d.damage() + (int) c.arg(1),
                d.unbreakable(), d.flags());
    }, int.class, int.class);

    public static final Map<String, Operator<ItemDescriptor>> OPERATORS = ops(OP_SET, OP_MOD);

    private ItemStat(String key) { super(key); }
    public static ItemStat itemStat(String key) { return new ItemStat(key); }
    
    @Override public Map<String, Operator<ItemDescriptor>> operators() { return OPERATORS; }
    @Override public Operator<ItemDescriptor> defaultOperator() { return OP_SET; }
}
