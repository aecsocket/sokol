package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * A visitor which applies a function to a {@link Rule}.
 */
@FunctionalInterface
public interface Visitor {
    /**
     * Applies a function to a rule.
     * @param rule The rule.
     */
    void visit(Rule rule);

    /**
     * Used when checking slot compatibility in {@link com.gitlab.aecsocket.sokol.core.component.Slot#compatible(TreeNode, TreeNode)}.
     * <p>
     * This visitor:
     * <ul>
     *     <li>sets the value of {@link NavigationRule.AsChild} to {@code child}</li>
     *     <li>sets the value of {@link NavigationRule.AsParent} to {@code parent}</li>
     * </ul>
     */
    class SlotCompatibility implements Visitor {
        private final TreeNode child;
        private final @Nullable TreeNode parent;

        public SlotCompatibility(TreeNode child, @Nullable TreeNode parent) {
            this.child = child;
            this.parent = parent;
        }

        public TreeNode child() { return child; }
        public Optional<TreeNode> parent() { return Optional.ofNullable(parent); }

        @Override
        public void visit(Rule rule) {
            if (rule instanceof NavigationRule.AsChild childRule)
                childRule.node(child);
            if (rule instanceof NavigationRule.AsParent parentRule) {
                if (parent == null)
                    throw new IllegalStateException("No parent node set");
                parentRule.node(parent);
            }
        }
    }
}