package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.inbuilt.ListStat;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import io.leangen.geantyref.TypeToken;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

public final class EffectsStat extends ListStat<PotionEffect> {
    public static final Operator<List<PotionEffect>> OP_SET = opSet(new TypeToken<>() {});
    public static final Operator<List<PotionEffect>> OP_ADD = opAdd(new TypeToken<>() {});

    public static final Map<String, Operator<List<PotionEffect>>> OPERATORS = ops(OP_SET, OP_ADD);

    private EffectsStat(String key) { super(key); }
    public static EffectsStat effectsStat(String key) { return new EffectsStat(key); }
    
    @Override public Map<String, Operator<List<PotionEffect>>> operators() { return OPERATORS; }
    @Override public Operator<List<PotionEffect>> defaultOperator() { return OP_SET; }
}
