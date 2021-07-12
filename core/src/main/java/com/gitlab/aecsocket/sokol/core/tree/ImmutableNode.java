package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ImmutableNode implements TreeNode {
    private final TreeNode handle;

    private ImmutableNode(TreeNode handle) {
        this.handle = handle;
    }

    public static ImmutableNode of(TreeNode handle) {
        return new ImmutableNode(handle);
    }

    @Override public Component value() { return handle.value(); }
    @Override public EventDispatcher<TreeEvent> events() { return handle.events(); }
    @Override public StatMap stats() { return handle.stats(); }
    @Override public boolean complete() { return handle.complete(); }
    @Override public Map<String, ? extends TreeNode> children() { return new HashMap<>(handle.children()); }
    @Override public Map<String, ? extends ChildSlot<?, ?>> slotChildren() { return handle.slotChildren(); }
    @Override public Map<String, ? extends System.Instance> systems() { return new HashMap<>(handle.systems()); }

    @Override
    public TreeNode build() {
        return handle;
    }

    @Override
    public void visit(Visitor<TreeNode, Slot> visitor, String... path) {
        handle.visit((parent, slot, child, p) -> visitor.visit(of(parent), slot, child == null ? null : new ImmutableNode(child), p), path);
    }

    @Override
    public Optional<? extends TreeNode> parent() {
        return handle.parent().map(ImmutableNode::of);
    }

    @Override
    public Optional<? extends Slot> slot() {
        return handle.slot();
    }

    @Override
    public String[] path() {
        return handle.path();
    }

    @Override
    public TreeNode root() {
        return of(handle.root());
    }

    @Override
    public TreeNode asRoot() { return handle.asRoot(); }

    @Override
    public Optional<? extends TreeNode> child(String key) {
        return handle.child(key).map(ImmutableNode::of);
    }
    @Override
    public Optional<? extends TreeNode> node(String... path) {
        return handle.node(path).map(ImmutableNode::of);
    }
}