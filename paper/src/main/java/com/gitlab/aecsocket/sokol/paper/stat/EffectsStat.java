package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import io.leangen.geantyref.TypeToken;
import org.bukkit.potion.PotionEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public final class EffectsStat extends ListStat<PotionEffect> {
    private EffectsStat(@Nullable List<PotionEffect> def, boolean required) {
        super(def, required);
    }

    @Override public TypeToken<StatDescriptor<List<PotionEffect>>> type() { return new TypeToken<>() {}; }
    public static EffectsStat effectsStat(@Nullable List<PotionEffect> def) { return new EffectsStat(def, false); }
    public static EffectsStat effectsStat() { return new EffectsStat(null, true); }
}
