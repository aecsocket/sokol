package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.Animation;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A stat type which stores an {@link Animation}.
 */
public final class AnimationStat extends BasicStat<Animation> {
    private AnimationStat(@Nullable Animation def) {
        super(new TypeToken<>() {}, def, (a, b) -> b, Animation::new);
    }

    public static AnimationStat animationStat(@Nullable Animation def) { return new AnimationStat(def); }
    public static AnimationStat animationStat() { return new AnimationStat(null); }
}
