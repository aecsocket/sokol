package com.gitlab.aecsocket.sokol.core.feature.util;

public interface Availability {
    boolean available();
    void delay(long ms);
}
