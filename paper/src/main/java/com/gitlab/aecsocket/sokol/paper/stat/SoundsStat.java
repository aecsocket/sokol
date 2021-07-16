package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Combiner;
import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.StatDescriptor.desc;

public final class SoundsStat extends BasicStat<StatDescriptor<List<PreciseSound>>> {
    public static final Map<String, Combiner<List<PreciseSound>>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<List<PreciseSound>>>())
            .put("=", (a, b) -> b)
            .put("+", (a, b) -> {
                a.addAll(b);
                return a;
            })
            .build();
    public static final String DEFAULT_OPERATOR = "=";

    private SoundsStat(@Nullable List<PreciseSound> def, boolean required) {
        super(new TypeToken<StatDescriptor<List<PreciseSound>>>() {},
                desc(def),
                required,
                (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                v -> v.copy(ArrayList::new));
    }

    public static SoundsStat soundsStat(@Nullable List<PreciseSound> def) { return new SoundsStat(def, false); }
    public static SoundsStat soundsStat() { return new SoundsStat(null, true); }
}
