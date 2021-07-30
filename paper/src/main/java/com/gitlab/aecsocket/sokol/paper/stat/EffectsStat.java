package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.ListStat;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Operators;
import io.leangen.geantyref.TypeToken;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public final class EffectsStat extends ListStat<PotionEffect> {
    public static final Operator<List<PotionEffect>> OP_SET = opSet(new TypeToken<>() {});
    public static final Operator<List<PotionEffect>> OP_ADD = opAdd(new TypeToken<>() {});

    public static final Operator<List<PotionEffect>> OP_DEF = OP_SET;
    public static final Operators<List<PotionEffect>> OPERATORS = Operators.operators(OP_DEF,
            OP_SET, OP_ADD);

    public static final class Serializer extends ListStat.Serializer<PotionEffect> {
        public static final Serializer INSTANCE = new Serializer();
        @Override protected Operators<List<PotionEffect>> operators() { return OPERATORS; }
    }

    private EffectsStat() { super(new TypeToken<>() {}, OP_DEF); }

    public static EffectsStat effectsStat() { return new EffectsStat(); }
}
