package com.gitlab.aecsocket.sokol.core.system.util;

public interface Availability {
    boolean available();
    void delay(long ms);
}
