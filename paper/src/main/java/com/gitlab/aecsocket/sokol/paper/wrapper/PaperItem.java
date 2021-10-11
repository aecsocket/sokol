package com.gitlab.aecsocket.sokol.paper.wrapper;

import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public record PaperItem(ItemStack handle) implements Item {
    @Override public int amount() {
        return handle.getAmount();
    }

    @Override
    public PaperItem amount(int amount) {
        handle.setAmount(amount);
        return this;
    }

    @Override
    public PaperItem add(int amount) {
        return amount(handle.getAmount() + amount);
    }

    @Override
    public Component name() {
        return handle.displayName();
    }

    @Override
    public PaperItem name(Component name) {
        handle.editMeta(meta -> meta.displayName(name));
        return this;
    }
}
