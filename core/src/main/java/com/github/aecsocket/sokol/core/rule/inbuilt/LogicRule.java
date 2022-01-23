package com.github.aecsocket.sokol.core.rule.inbuilt;

import com.github.aecsocket.sokol.core.api.BaseNode;
import com.github.aecsocket.sokol.core.api.Node;
import com.github.aecsocket.sokol.core.rule.base.BaseRule;
import com.github.aecsocket.sokol.core.rule.base.BaseRuleException;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.rule.node.NodeRuleException;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class LogicRule {
    private LogicRule() {}

    public static final class Constant implements BaseRule, NodeRule {
        public static final Constant TRUE = new Constant(true);
        public static final Constant FALSE = new Constant(false);

        private final boolean value;

        private Constant(boolean value) {
            this.value = value;
        }

        public static Constant of(boolean value) {
            return value ? TRUE : FALSE;
        }

        public boolean value() { return value; }

        @Override
        public <N extends BaseNode.Scoped<N, ?>> void applies(N target, N parent) throws BaseRuleException {
            if (!value)
                throw new BaseRuleException(this);
        }

        @Override
        public <N extends Node.Scoped<N, ?, ?, ?, ?>> void applies(N target, @Nullable N parent) throws NodeRuleException {
            if (!value)
                throw new NodeRuleException(this);
        }
    }

    public static final class Not implements BaseRule, NodeRule {
        private final

        private Not() {}
    }
}
