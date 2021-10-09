package com.gitlab.aecsocket.sokol.core.registry;

import net.kyori.adventure.text.Component;

import java.util.Locale;

public interface Keyed {

    String VALID = "abcdefghijklmnopqrstuvwxyz0123456789._-";

    static String validate(String key) throws ValidationException {
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!VALID.contains(""+c))
                throw new ValidationException(i, c);
        }
        return key;
    }

    String id();
    Component name(Locale locale);
}
