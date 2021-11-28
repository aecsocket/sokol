package com.gitlab.aecsocket.sokol.core.nodeview;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.Slot;

public interface OffsetSlot extends Slot {
    Point2 offset();
}
