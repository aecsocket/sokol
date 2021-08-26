package com.gitlab.aecsocket.sokol.core.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
import com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;

import java.util.Locale;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat.*;

/**
 * A system which can represent a tree node as an {@link ItemStack}.
 */
public abstract class ItemSystem extends AbstractSystem {
    /** The system ID. */
    public static final String ID = "item";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    /**
     * {@code item_name}: The localization key for an item name.
     * If unset, uses {@link com.gitlab.aecsocket.sokol.core.component.Component#name(Locale)} to generate the name.
     */
    public static final StringStat STAT_ITEM_NAME = stringStat("item_name");
    /** {@code item}. */
    public static final String KEY_ITEM = "item";
    /** The stat types. */
    public static final StatTypes STATS = StatTypes.of(STAT_ITEM_NAME);

    protected final StatTypes statTypes;

    public ItemSystem(Stat<? extends ItemStack.Factory> statItem) {
        super(0);
        statTypes = new StatTypes()
                .putAll(STATS)
                .put(statItem);
    }

    /**
     * See {@link ItemSystem}.
     */
    public abstract class Instance extends AbstractSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract ItemSystem base();

        /**
         * Creates an item stack representation of the parent node.
         * @param locale The locale to create the representation for.
         * @return The item stack.
         */
        public ItemStack create(Locale locale) {
            ItemStack.Factory factory = parent.stats().req(KEY_ITEM);
            ItemStack item = factory.create();
            item.save(parent);
            item.name(parent.stats().val(STAT_ITEM_NAME)
                    .map(k -> platform().lc().safe(locale, k))
                    .orElse(parent.value().name(locale)));
            new Events.CreateItem(this, locale, item).call();
            return item;
        }
    }

    @Override public String id() { return ID; }
    @Override public StatTypes statTypes() { return statTypes; }

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
