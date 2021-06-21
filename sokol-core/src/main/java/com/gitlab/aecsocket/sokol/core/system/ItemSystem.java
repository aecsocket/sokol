package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.tree.TreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class ItemSystem<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends AbstractSystem<N> {
    public static final String ID = "item";
    private final Map<String, Stat<?>> baseStats;

    public ItemSystem(Stat<? extends ItemStack.Factory> itemStat) {
        baseStats = CollectionBuilder.map(new HashMap<String, Stat<?>>())
                .put("item", itemStat)
                .build();
    }

    public static abstract class Instance<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends AbstractSystem.Instance<N> {
        public Instance(N parent) {
            super(parent);
        }

        @Override
        public void build() {}

        public ItemStack create(Locale locale) {
            ItemStack.Factory factory = stat("item");
            if (factory == null)
                throw new IllegalArgumentException("No item provided");
            ItemStack item = factory.create();
            item.save(parent());
            item.name(parent().value().name(locale));
            call(new Events.CreateItem(this, locale, item));
            return item;
        }
    }

    @Override public @NotNull String id() { return ID; }
    @Override public @NotNull Map<String, Stat<?>> baseStats() { return baseStats; }
    @Override public @NotNull Map<String, Class<? extends Rule>> ruleTypes() { return Collections.emptyMap(); }

    public static final class Events {
        private Events() {}

        public static class CreateItem implements TreeEvent.SystemEvent<Instance<?>> {
            private final Instance<?> system;
            private final Locale locale;
            private final ItemStack item;

            public CreateItem(Instance<?> system, Locale locale, ItemStack item) {
                this.system = system;
                this.locale = locale;
                this.item = item;
            }

            @Override public Instance<?> system() { return system; }
            public Locale locale() { return locale; }
            public ItemStack item() { return item; }
        }
    }
}
