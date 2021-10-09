package com.gitlab.aecsocket.sokol.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Pools {
    private Pools() {}

    private static final Map<Locale, DecimalFormat> decimalFormatters = new HashMap<>();
    public static DecimalFormat decimalFormatter(Locale locale) {
        return decimalFormatters.computeIfAbsent(locale, l -> new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(l)));
    }
}
