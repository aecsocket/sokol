package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.ListStat;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Operators;
import io.leangen.geantyref.TypeToken;

import java.util.*;

public final class SoundsStat extends ListStat<PreciseSound> {
    public static final Operator<List<PreciseSound>> OP_SET = opSet(new TypeToken<>() {});
    public static final Operator<List<PreciseSound>> OP_ADD = opAdd(new TypeToken<>() {});

    public static final Operator<List<PreciseSound>> OP_DEF = OP_SET;
    public static final Operators<List<PreciseSound>> OPERATORS = Operators.operators(OP_DEF,
            OP_SET, OP_ADD);
    
    public static final class Serializer extends ListStat.Serializer<PreciseSound> {
        public static final Serializer INSTANCE = new Serializer();
        @Override protected Operators<List<PreciseSound>> operators() { return OPERATORS; }
    }

    private SoundsStat() { super(new TypeToken<>() {}, OP_DEF); }

    public static SoundsStat soundsStat() { return new SoundsStat(); }
}
