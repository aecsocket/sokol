package com.gitlab.aecsocket.sokol.paper.wrapper;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<Component> description() {
        return handle.getItemMeta().lore();
    }

    @Override
    public PaperItem description(List<Component> description) {
        handle.editMeta(meta -> meta.lore(description));
        return this;
    }

    @Override
    public PaperItem addDescription(List<Component> description) {
        handle.editMeta(meta -> {
            List<Component> lore = meta.lore();
            if (lore == null || lore.size() == 0)
                meta.lore(description.stream().map(Components.BLANK::append).collect(Collectors.toList()));
            else {
                lore.add(Component.empty());
                lore.addAll(description.stream().map(Components.BLANK::append).collect(Collectors.toList()));
                meta.lore(lore);
            }
        });
        return this;
    }
}
