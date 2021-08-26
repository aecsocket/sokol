package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.inbuilt.ListStat;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import io.leangen.geantyref.TypeToken;

import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

public final class SoundsStat extends ListStat<PreciseSound> {
    public static final Operator<List<PreciseSound>> OP_SET = opSet(new TypeToken<>() {});
    public static final Operator<List<PreciseSound>> OP_ADD = opAdd(new TypeToken<>() {});

    public static final Map<String, Operator<List<PreciseSound>>> OPERATORS = ops(OP_SET, OP_ADD);

    private SoundsStat(String key) { super(key); }
    public static SoundsStat soundsStat(String key) { return new SoundsStat(key); }

    @Override public Map<String, Operator<List<PreciseSound>>> operators() { return OPERATORS; }
    @Override public Operator<List<PreciseSound>> defaultOperator() { return OP_SET; }
}
