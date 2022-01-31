package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.event.ItemEvent;
import com.github.aecsocket.sokol.core.event.NodeEvent;

public final class PaperEvents {
    private PaperEvents() {}

    public record CreateItem(
        PaperTreeNode node,
        PaperItemStack item
    ) implements NodeEvent.CreateItem<PaperTreeNode, PaperBlueprintNode, PaperItemStack> {}

    public record Hold(PaperTreeNode node) implements ItemEvent.Hold<PaperTreeNode> {}
}
