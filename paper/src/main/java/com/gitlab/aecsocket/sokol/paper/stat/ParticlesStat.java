package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.paper.display.Particles;
import com.gitlab.aecsocket.sokol.core.stat.ListStat;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Operators;
import io.leangen.geantyref.TypeToken;

import java.util.List;

public final class ParticlesStat extends ListStat<Particles> {
    public static final Operator<List<Particles>> OP_SET = opSet(new TypeToken<>() {});
    public static final Operator<List<Particles>> OP_ADD = opAdd(new TypeToken<>() {});

    public static final Operator<List<Particles>> OP_DEF = OP_SET;
    public static final Operators<List<Particles>> OPERATORS = Operators.operators(OP_DEF,
            OP_SET, OP_ADD);

    public static final class Serializer extends ListStat.Serializer<Particles> {
        public static final Serializer INSTANCE = new Serializer();
        @Override protected Operators<List<Particles>> operators() { return OPERATORS; }
    }

    private ParticlesStat() { super(new TypeToken<>() {}, OP_DEF); }

    public static ParticlesStat particlesStat() { return new ParticlesStat(); }
}
