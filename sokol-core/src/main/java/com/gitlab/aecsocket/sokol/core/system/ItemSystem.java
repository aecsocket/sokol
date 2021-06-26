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

public abstract class ItemSystem extends AbstractSystem {
    public static final String ID = "item";
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

    public static abstract class Instance extends AbstractSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

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
    @Override public @NotNull Map<String, Stat<?>> baseStats() { return baseStats; }

    public static final class Events {
        private Events() {}

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
            public Locale locale() { return locale; }
            public ItemStack item() { return item; }
        }
    }
}
