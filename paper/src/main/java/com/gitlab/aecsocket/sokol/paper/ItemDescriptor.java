package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Arrays;

@ConfigSerializable
public record ItemDescriptor(
        NamespacedKey key,
        int modelData,
        int damage,
        boolean unbreakable,
        ItemFlag[] flags
) {
    public ItemStack buildStack() throws ItemCreationException {
        Material material = Registry.MATERIAL.get(key);
        if (material == null)
            throw new ItemCreationException("No material with key " + key);
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.addItemFlags(flags);
            meta.setCustomModelData(modelData);
            meta.setUnbreakable(unbreakable);
            if (meta instanceof Damageable dmg)
                dmg.setDamage(damage);
        });
        return item;
    }

    public PaperItem buildWrapper() throws ItemCreationException {
        return new PaperItem(buildStack());
    }

    @Override
    public String toString() {
        return key + "(" + modelData + ", " + damage + ")" + Arrays.toString(flags) +
                (unbreakable ? "<unbreakable>" : "");
    }
}
