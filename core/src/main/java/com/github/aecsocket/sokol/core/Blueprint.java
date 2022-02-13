package com.github.aecsocket.sokol.core;

import com.github.aecsocket.sokol.core.registry.Keyed;

public interface Blueprint<
    N extends BlueprintNode.Scoped<N, ?, ?, ?>
> extends Keyed {
    String I18N_KEY = "blueprint";

    N create();

    @Override default String i18nBase() { return I18N_KEY; }
}
