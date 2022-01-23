package com.github.aecsocket.sokol.core.registry;

import com.gitlab.aecsocket.minecommons.core.i18n.I18N;
import com.gitlab.aecsocket.minecommons.core.i18n.Renderable;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface Keyed extends Renderable {
    String VALID = "abcdefghijklmnopqrstuvwxyz0123456789._-";
    String NAME = "name";
    String DESCRIPTION = "description";

    static String validate(String key) throws ValidationException {
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!VALID.contains(""+c))
                throw new ValidationException(i, c);
        }
        return key;
    }

    String id();

    Optional<List<Component>> renderDescription(I18N i18n, Locale locale);
}
