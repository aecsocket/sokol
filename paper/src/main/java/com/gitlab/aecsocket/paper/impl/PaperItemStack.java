package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.world.ItemStack;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.Numbers;
import com.gitlab.aecsocket.paper.SokolPlugin;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public record PaperItemStack(
        SokolPlugin platform,
        org.bukkit.inventory.ItemStack handle
) implements ItemStack.Scoped<PaperItemStack, PaperBlueprint> {
    @Override
    public Optional<PaperBlueprint> asBlueprint() {
        return platform.persistence().safeLoad(handle.getItemMeta().getPersistentDataContainer());
    }

    @Override
    public int amount() { return handle.getAmount(); }

    @Override
    public PaperItemStack amount(int amount) {
        handle.setAmount(amount);
        return this;
    }

    @Override
    public PaperItemStack add(int amount) {
        handle.setAmount(handle.getAmount() + amount);
        return this;
    }

    @Override
    public Component name() { return handle.getItemMeta().displayName(); }

    @Override
    public PaperItemStack name(Component name) {
        handle.editMeta(meta -> meta.displayName(Components.BLANK.append(name)));
        return this;
    }

    @Override
    public List<Component> lore() { return handle.getItemMeta().lore(); }

    @Override
    public PaperItemStack lore(List<Component> lore) {
        handle.editMeta(meta -> meta.lore(lore));
        return this;
    }

    @Override
    public PaperItemStack addLore(List<Component> lore) {
        handle.editMeta(meta -> {
            List<Component> existing = meta.lore();
            if (existing == null) existing = new ArrayList<>();
            if (existing.size() > 0)
                existing.add(Component.empty()); // spacer
            for (var line : lore)
                existing.add(Components.BLANK.append(line));
            meta.lore(lore);
        });
        return this;
    }

    @Override
    public OptionalDouble durability() {
        if (handle.getItemMeta() instanceof Damageable meta)
            return OptionalDouble.of(1 - ((double) meta.getDamage() / handle.getType().getMaxDurability()));
        return OptionalDouble.empty();
    }

    @Override
    public PaperItemStack durability(double percent) {
        handle.editMeta(m -> {
            if (m instanceof Damageable meta) {
                int max = handle.getType().getMaxDurability();
                int damage = (int) Numbers.clamp(max * (1 - percent), 1, max - 1);
                meta.setDamage(damage);
            }
        });
        return this;
    }

    @Override
    public PaperItemStack repair() {
        handle.editMeta(m -> {
            if (m instanceof Damageable meta)
                meta.setDamage(0);
        });
        return this;
    }
}
