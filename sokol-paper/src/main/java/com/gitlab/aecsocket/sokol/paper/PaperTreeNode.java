package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.tree.BasicTreeNode;

import java.util.Map;

public class PaperTreeNode extends BasicTreeNode<PaperTreeNode, PaperComponent, PaperSystem, PaperSystem.Instance> {
    public PaperTreeNode(PaperComponent value, Map<String, PaperTreeNode> children, Map<String, PaperSystem.Instance> systems, String key, PaperTreeNode parent) {
        super(value, children, systems, key, parent);
    }

    public PaperTreeNode(PaperComponent value, Map<String, PaperTreeNode> children, Map<String, PaperSystem.Instance> systems) {
        super(value, children, systems);
    }

    public PaperTreeNode(PaperComponent value) {
        super(value);
    }
}
