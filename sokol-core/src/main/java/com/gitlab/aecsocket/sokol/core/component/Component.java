package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A base value of a {@link com.gitlab.aecsocket.sokol.core.tree.TreeNode}, storing the slots and base systems.
 */
public interface Component extends Keyed {
    /**
     * A scoped version of a component.
     * @param <C> The type of this component.
     * @param <S> The slot type.
     * @param <B> The base system type.
     */
    interface Scoped<C extends Scoped<C, S, B>, S extends Slot, B extends System> extends Component {
        @Override @NotNull Map<String, S> slots();
        @Override Optional<S> slot(@NotNull String key);

        @Override @NotNull Map<String, B> baseSystems();
        @Override Optional<B> baseSystem(@NotNull String id);
    }

    /**
     * Gets the platform that this component uses.
     * @return The platform.
     */
    @NotNull SokolPlatform platform();

    /**
     * Gets the localized name of this component.
     * @param locale The locale to localize for.
     * @return The name.
     */
    default @NotNull net.kyori.adventure.text.Component name(@NotNull Locale locale) {
        return platform().localize(locale, "component." + id());
    }

    /**
     * Gets all of the tags of this component.
     * @return The tags.
     */
    @NotNull Collection<String> tags();

    /**
     * Gets if this component is tagged with a specific tag.
     * @param tag The tag.
     * @return The result.
     */
    boolean tagged(@NotNull String tag);

    /**
     * Gets all of the slots mapped to their keys.
     * @return The slots.
     */
    @NotNull Map<String, ? extends Slot> slots();

    /**
     * Gets a slot by its key.
     * @param key The key.
     * @return An Optional of the result.
     */
    Optional<? extends Slot> slot(@NotNull String key);

    /**
     * Gets all of the base systems mapped to their IDs.
     * @return The base systems.
     */
    @NotNull Map<String, ? extends System> baseSystems();

    /**
     * Gets a system by its ID.
     * @param id The ID.
     * @return An Optional of the result.
     */
    Optional<? extends System> baseSystem(@NotNull String id);

    /**
     * Gets all of the stats of this component, used in building a tree node's stats.
     * @return The stats.
     */
    @NotNull StatLists stats();
}
