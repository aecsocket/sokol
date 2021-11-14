package com.gitlab.aecsocket.sokol.paper.event;

import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.sokol.core.event.ItemEvent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperItemSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;

public interface PaperItemEvent extends ItemEvent<PaperNode, PaperItem> {
    @Override PaperUser user();
    @Override PaperItemSlot slot();

    record Hold(
            PaperNode node,
            PaperUser user,
            PaperItemSlot slot,
            PaperItem item,
            boolean sync,
            TaskContext context
    ) implements PaperItemEvent, ItemEvent.Hold<PaperNode, PaperItem> {}
}
