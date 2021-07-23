package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public final class SoundsStat extends ListStat<PreciseSound> {
    private SoundsStat(@Nullable List<PreciseSound> def, boolean required) {
        super(def, required);
    }

    @Override public TypeToken<StatDescriptor<List<PreciseSound>>> type() { return new TypeToken<>() {}; }
    public static SoundsStat soundsStat(@Nullable List<PreciseSound> def) { return new SoundsStat(def, false); }
    public static SoundsStat soundsStat() { return new SoundsStat(null, true); }
}
