package com.gitlab.aecsocket.sokol.core.stat;

import java.util.ArrayList;
import java.util.List;

public final class StatIntermediate {
    public record Priority(int value, boolean reverse) {
        @Override
        public String toString() {
            return reverse ? "(" + value + ")" : value+"";
        }
    }

    public static Priority forwardPriority(int value) {
        return new Priority(value, false);
    }

    public static Priority reversePriority(int value) {
        return new Priority(value, true);
    }

    public static Priority minPriority() {
        return new Priority(Integer.MIN_VALUE, false);
    }

    public static Priority maxPriority() {
        return new Priority(Integer.MAX_VALUE, true);
    }

    private record MapData(StatMap map, Priority priority) {}

    private final List<MapData> forward;
    private final List<MapData> reverse;

    public StatIntermediate(List<MapData> forward, List<MapData> reverse) {
        this.forward = forward;
        this.reverse = reverse;
    }

    public StatIntermediate(StatIntermediate o) {
        forward = new ArrayList<>();
        for (var map : o.forward)
            forward.add(new MapData(new StatMap(map.map), map.priority));
        reverse = new ArrayList<>();
        for (var map : o.reverse)
            reverse.add(new MapData(new StatMap(map.map), map.priority));
    }

    public StatIntermediate() {
        forward = new ArrayList<>();
        reverse = new ArrayList<>();
    }

    public List<MapData> forward() { return forward; }
    public List<MapData> reverse() { return reverse; }

    public void addForward(StatMap map, Priority priority) {
        forward.add(new MapData(map, priority));
    }

    public void addReverse(StatMap map, Priority priority) {
        reverse.add(new MapData(map, priority));
    }
}
