package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StringStat;
import com.gitlab.aecsocket.sokol.core.tree.TreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A system which can represent a tree node as an {@link ItemStack}.
 */
public abstract class ItemSystem extends AbstractSystem {
    /** The system ID. */
    public static final String ID = "item";
    /**
     * The stat types.
     * <ul>
     *     <li>{@link StringStat} {@code item_name}: The localization key for an item. If unset, uses
     *     {@link com.gitlab.aecsocket.sokol.core.component.Component#name(Locale)} to generate the name.</li>
     * </ul>
     */
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("item_name", new StringStat())
            .build();

    private final Map<String, Stat<?>> baseStats;

    public ItemSystem(Stat<? extends ItemStack.Factory> itemStat) {
        baseStats = CollectionBuilder.map(new HashMap<String, Stat<?>>())
                .put(STATS)
                .put("item", itemStat)
                .build();
    }

    /**
     * See {@link ItemSystem}.
     */
    public static abstract class Instance extends AbstractSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        /**
         * Creates an item stack representation of the parent node.
         * @param locale The locale to create the representation for.
         * @return The item stack.
         */
        public ItemStack create(Locale locale) {
            ItemStack.Factory factory = parent.stats().value("item");
            if (factory == null)
                throw new IllegalArgumentException("No item provided");
            ItemStack item = factory.create();
            item.save(parent);
            item.name(parent.stats().<String>optValue("item_name")
                    .map(k -> platform().localize(locale, k))
                    .orElse(parent.value().name(locale)));
            new Events.CreateItem(this, locale, item).call();
            return item;
        }
    }

    @Override public @NotNull String id() { return ID; }
    @Override public @NotNull Map<String, Stat<?>> statTypes() { return baseStats; }

    /**
     * This system's event types.
     */
    public static final class Events {
        private Events() {}

        /**
         * Represents an item being created.
         */
        public static class CreateItem implements TreeEvent.SystemEvent<Instance> {
            private final Instance system;
            private final Locale locale;
            private final ItemStack item;

            public CreateItem(Instance system, Locale locale, ItemStack item) {
                this.system = system;
                this.locale = locale;
                this.item = item;
            }

            @Override public Instance system() { return system; }

            /**
             * Gets the locale used to create the item stack.
             * @return The locale.
             */
            public Locale locale() { return locale; }

            /**
             * Gets the current state of the item stack being created.
             * @return The item stack.
             */
            public ItemStack item() { return item; }
        }
    }
}
