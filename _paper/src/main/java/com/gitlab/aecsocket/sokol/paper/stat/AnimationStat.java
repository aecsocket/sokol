package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.Animation;

import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

/**
 * A stat type which stores an {@link Animation}.
 */
public final class AnimationStat extends Stat<Animation> {
    public static final Operator<Animation> OP_SET = op("=", c -> c.arg(0), Animation.class);

    public static final Map<String, Operator<Animation>> OPERATORS = ops(OP_SET);

    private AnimationStat(String key) { super(key); }
    public static AnimationStat animationStat(String key) { return new AnimationStat(key); }

    @Override public Map<String, Operator<Animation>> operators() { return OPERATORS; }
    @Override public Operator<Animation> defaultOperator() { return OP_SET; }
}
