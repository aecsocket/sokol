package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Descriptor;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Operators;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import io.leangen.geantyref.TypeToken;

/**
 * A stat type which stores an {@link ItemDescriptor}.
 */
public final class ItemStat extends BasicStat<ItemDescriptor> {
    public static final Operator<ItemDescriptor> OP_SET = op("=", c -> c.arg(0), ItemDescriptor.class);

    public static final Operator<ItemDescriptor> OP_DEF = OP_SET;
    public static final Operators<ItemDescriptor> OPERATORS = Operators.operators(OP_DEF, OP_SET);

    public static final class Serializer extends Descriptor.Serializer<ItemDescriptor> {
        public static final Serializer INSTANCE = new Serializer();
        @Override protected Operators<ItemDescriptor> operators() { return OPERATORS; }
    }

    private ItemStat() { super(new TypeToken<>() {}, OP_DEF); }

    public static ItemStat itemStat() { return new ItemStat(); }
}
