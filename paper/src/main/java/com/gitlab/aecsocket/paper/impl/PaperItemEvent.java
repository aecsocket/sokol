package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.event.ItemEvent;

public final class PaperItemEvent {
    private PaperItemEvent() {}

    public record Hold(PaperNode node) implements ItemEvent.Hold<PaperNode> {}
}
