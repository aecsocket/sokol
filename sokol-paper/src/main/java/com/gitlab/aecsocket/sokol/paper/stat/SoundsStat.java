package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Combiner;
import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import io.leangen.geantyref.TypeToken;

import java.util.*;

public final class SoundsStat extends BasicStat<StatDescriptor<List<PreciseSound>>> {
    public static final Map<String, Combiner<List<PreciseSound>>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<List<PreciseSound>>>())
            .put("=", (a, b) -> b)
            .put("+", (a, b) -> {
                a.addAll(b);
                return a;
            })
            .build();
    public static final String DEFAULT_OPERATOR = "=";

    public SoundsStat(List<PreciseSound> defaultValue) {
        super(new TypeToken<StatDescriptor<List<PreciseSound>>>() {},
                new StatDescriptor<>(defaultValue),
                (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                v -> v.copy(ArrayList::new));
    }

    public SoundsStat() {
        this(new ArrayList<>());
    }
}
