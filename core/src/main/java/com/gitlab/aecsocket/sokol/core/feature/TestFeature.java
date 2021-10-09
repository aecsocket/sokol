package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.event.ItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Primitives;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import io.leangen.geantyref.TypeToken;

public abstract class TestFeature<N extends Node.Scoped<N, ?, ?>> extends AbstractFeature<TestFeature<N>.Instance, N> {
    public static final String ID = "test";
    public static final Primitives.OfFlag STAT_SOME_FLAG = Primitives.flagStat("some_flag");
    public static final Primitives.OfDecimal STAT_INACCURACY = Primitives.decimalStat("inaccuracy");

    public abstract class Instance extends AbstractInstance<N> {
        public Instance(N parent) {
            super(parent);
        }

        @Override
        public TestFeature<N> type() { return TestFeature.this; }

        interface ItemUserImpl extends ItemUser {
            boolean flag();
        }

        @Override
        public void build(NodeEvent<N> event, StatIntermediate stats) {
            EventDispatcher<NodeEvent<N>> events = parent.events();
            if (event instanceof ItemEvent<N, ?> itemEvent) {
                if (itemEvent.user() instanceof ItemUserImpl user) {
                    if (user.flag()) {
                        stats.addReverse(new StatMap()
                                        .set(STAT_SOME_FLAG, STAT_SOME_FLAG.set(true))
                                        .set(STAT_INACCURACY, STAT_INACCURACY.multiply(2)),
                                StatIntermediate.Priority.MAX,
                                Rule.Constant.TRUE);
                    }
                }
            }
            events.register(new TypeToken<Events.TestEvent<N>>(){}, this::handle);
        }

        protected void handle(Events.TestEvent<N> event) {

        }
    }

    @Override public String id() { return ID; }

    public static final class Events {
        private Events() {}

        public record TestEvent<N extends Node.Scoped<N, ?, ?>>(
                N node,
                int value
        ) implements NodeEvent<N> {}
    }
}
