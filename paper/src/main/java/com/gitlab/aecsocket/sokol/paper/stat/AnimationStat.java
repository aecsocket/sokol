package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Descriptor;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Operators;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.Animation;
import io.leangen.geantyref.TypeToken;

/**
 * A stat type which stores an {@link Animation}.
 */
public final class AnimationStat extends BasicStat<Animation> {
    public static final Operator<Animation> OP_SET = op("=", c -> c.arg(0), Animation.class);

    public static final Operator<Animation> OP_DEF = OP_SET;
    public static final Operators<Animation> OPERATORS = Operators.operators(OP_DEF, OP_SET);

    public static final class Serializer extends Descriptor.Serializer<Animation> {
        public static final Serializer INSTANCE = new Serializer();
        @Override protected Operators<Animation> operators() { return OPERATORS; }
    }

    private AnimationStat() { super(new TypeToken<>() {}, OP_DEF); }

    public static AnimationStat animationStat() { return new AnimationStat(); }
}
