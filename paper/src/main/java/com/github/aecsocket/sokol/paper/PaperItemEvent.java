package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.event.ItemEvent;

public final class PaperItemEvent {
    private PaperItemEvent() {}

    public record Hold(PaperTreeNode node) implements ItemEvent.Hold<PaperTreeNode> {}
}
