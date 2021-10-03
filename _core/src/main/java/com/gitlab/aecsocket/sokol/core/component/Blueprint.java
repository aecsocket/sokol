package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.Optional;

public class Blueprint<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> implements Keyed {
    protected final SokolPlatform platform;
    protected final String id;
    protected final N node;

    public Blueprint(SokolPlatform platform, String id, N node) {
        this.platform = platform;
        this.id = id;
        this.node = node;
    }

    public SokolPlatform platform() { return platform; }
    @Override public String id() { return id; }

    /**
     * Gets the localized name of this blueprint.
     * @param locale The locale to localize for.
     * @return The name.
     */
    public Component name(Locale locale) {
        return platform.lc().safe(locale, "blueprint." + id);
    }

    public N node() { return node; }

    public N build() { return node.asRoot(); }
}
