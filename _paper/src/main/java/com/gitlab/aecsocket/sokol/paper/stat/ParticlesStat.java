package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.paper.display.Particles;
import com.gitlab.aecsocket.sokol.core.stat.inbuilt.ListStat;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import io.leangen.geantyref.TypeToken;

import java.util.List;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

public final class ParticlesStat extends ListStat<Particles> {
    public static final Operator<List<Particles>> OP_SET = opSet(new TypeToken<>() {});
    public static final Operator<List<Particles>> OP_ADD = opAdd(new TypeToken<>() {});

    public static final Map<String, Operator<List<Particles>>> OPERATORS = ops(OP_SET, OP_ADD);

    private ParticlesStat(String key) { super(key); }
    public static ParticlesStat particlesStat(String key) { return new ParticlesStat(key); }
    
    @Override public Map<String, Operator<List<Particles>>> operators() { return OPERATORS; }
    @Override public Operator<List<Particles>> defaultOperator() { return OP_SET; }
}
