package com.github.aecsocket.sokol.core.nodeview;

import com.github.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.github.aecsocket.sokol.core.NodeSlot;

public interface OffsetNodeSlot extends NodeSlot {
    Point2 offset();
}
