package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.component.BasicSlot;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@ConfigSerializable
public final class PaperSlot extends BasicSlot<PaperComponent> {
    private final Point2 offset;

    public PaperSlot(Collection<String> tags, String key, PaperComponent parent, Point2 offset) {
        super(tags, key, parent);
        this.offset = offset;
    }

    public PaperSlot(Collection<String> tags, Point2 offset) {
        super(tags);
        this.offset = offset;
    }

    private PaperSlot() {
        this(Collections.emptySet(), Point2.ZERO);
    }

    public Point2 offset() { return offset; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PaperSlot paperSlot = (PaperSlot) o;
        return offset.equals(paperSlot.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), offset);
    }

    @Override
    public String toString() {
        return "PaperSlot{" +
                "tags=" + tags +
                ", offset=" + offset +
                '}';
    }
}
