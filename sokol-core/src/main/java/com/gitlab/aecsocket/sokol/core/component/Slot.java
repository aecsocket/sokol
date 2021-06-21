package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;

public interface Slot {
    String TAG_REQUIRED = "required";
    String TAG_INTERNAL = "internal";
    String TAG_FIELD_MODIFIABLE = "field_modifiable";

    @NotNull SokolPlatform platform();

    default @NotNull net.kyori.adventure.text.Component name(Locale locale) {
        return platform().localize(locale, "slot." + key());
    }

    Collection<String> tags();
    boolean tagged(String tag);

    @NotNull Rule rule();

    @Contract("_, null -> true")
    boolean compatible(@Nullable TreeNode parent, @Nullable TreeNode child);

    default boolean required() { return tagged(TAG_REQUIRED); }
    default boolean internal() { return tagged(TAG_INTERNAL); }
    default boolean fieldModifiable() { return tagged(TAG_FIELD_MODIFIABLE); }

    @NotNull String key();
    @NotNull Component parent();
}
