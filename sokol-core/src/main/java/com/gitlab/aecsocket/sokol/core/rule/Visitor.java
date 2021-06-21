package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;

public interface Visitor {
    void visit(Rule rule);

    class SlotCompatibility implements Visitor {
        private final TreeNode child;
        private final TreeNode parent;

        public SlotCompatibility(TreeNode child, TreeNode parent) {
            this.child = child;
            this.parent = parent;
        }

        public TreeNode child() { return child; }
        public TreeNode parent() { return parent; }

        @Override
        public void visit(Rule rule) {
            if (rule instanceof NavigationRule.AsChild childRule)
                childRule.node(child);
            if (rule instanceof NavigationRule.AsParent parentRule)
                parentRule.node(parent);
        }
    }
}
