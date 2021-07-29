package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.paper.display.Particles;
import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public final class ParticlesStat extends ListStat<Particles> {
    private ParticlesStat(@Nullable List<Particles> def) {
        super(def);
    }

    @Override public TypeToken<StatDescriptor<List<Particles>>> type() { return new TypeToken<>() {}; }
    public static ParticlesStat particlesStat(@Nullable List<Particles> def) { return new ParticlesStat(def); }
    public static ParticlesStat particlesStat() { return new ParticlesStat(null); }
}
