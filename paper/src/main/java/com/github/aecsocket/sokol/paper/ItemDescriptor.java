package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.world.ItemCreationException;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.Arrays;

@ConfigSerializable
public record ItemDescriptor(
    @Required NamespacedKey key,
    int modelData,
    int damage,
    boolean unbreakable,
    ItemFlag[] flags
) {
    public ItemStack stack() throws ItemCreationException {
        Material material = Registry.MATERIAL.get(key);
        if (material == null)
            throw new ItemCreationException("No material with key `" + key + "`");
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.addItemFlags(flags);
            meta.setCustomModelData(modelData);
            meta.setUnbreakable(unbreakable);
            if (meta instanceof Damageable dmg)
                dmg.setDamage(damage);
        });
        return stack;
    }

    public PaperItemStack wrapper(SokolPlugin plugin) throws ItemCreationException {
        return new PaperItemStack(plugin, stack());
    }

    @Override
    public String toString() {
        return key + "(mdl=" + modelData + ", dmg=" + damage + ")" + (flags.length == 0 ? "" : Arrays.toString(flags)) +
                (unbreakable ? " <unbreakable>" : "");
    }
}
