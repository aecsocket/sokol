package com.github.aecsocket.sokol.paper;

import java.util.*;

import com.github.aecsocket.minecommons.core.Components;
import com.github.aecsocket.minecommons.core.Numbers;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import net.kyori.adventure.text.Component;

public record PaperItemStack(
    SokolPlugin plugin,
    ItemStack handle
) implements com.github.aecsocket.sokol.core.world.ItemStack.Scoped<
    PaperItemStack,
    PaperBlueprintNode
> {
    @Override
    public Optional<PaperBlueprintNode> asBlueprint() {
        return plugin.persistence().load(handle);
    }

    @Override
    public int amount() {
        return handle.getAmount();
    }

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
    public Component name() {
        return handle.getItemMeta().displayName();
    }

    @Override
    public PaperItemStack name(Component name) {
        handle.editMeta(meta -> meta.displayName(Components.BLANK.append(name)));
        return this;
    }

    @Override
    public List<Component> lore() {
        return handle.getItemMeta().lore();
    }

    @Override
    public PaperItemStack lore(List<Component> lore) {
        handle.editMeta(meta -> meta.lore(lore));
        return this;
    }

    @Override
    public PaperItemStack addLore(Locale locale, List<Component> lore) {
        if (lore.isEmpty())
            return this;
        handle.editMeta(meta -> {
            List<Component> cur = meta.lore();
            if (cur == null)
                cur = new ArrayList<>();
            if (!cur.isEmpty())
                cur.addAll(plugin.i18n().lines(locale, ITEM_LORE_SEPARATOR));
            
            for (var line : lore) {
                cur.add(Components.BLANK.append(line));
            }
            meta.lore(cur);
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
        handle.editMeta(Damageable.class, meta -> {
            int max = handle.getType().getMaxDurability();
            meta.setDamage((int) Numbers.clamp(max * (1 - percent), 1, max - 1));
        });
        return this;
    }

    @Override
    public PaperItemStack repair() {
        handle.editMeta(Damageable.class, meta -> {
            meta.setDamage(0);
        });
        return this;
    }
}
