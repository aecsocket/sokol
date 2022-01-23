package com.github.aecsocket.sokol.core.rule.node;

import com.github.aecsocket.sokol.core.api.Node;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface NodeRule {
    <N extends Node.Scoped<N, ?, ?, ?, ?>> void applies(N target, @Nullable N parent) throws NodeRuleException;
}
