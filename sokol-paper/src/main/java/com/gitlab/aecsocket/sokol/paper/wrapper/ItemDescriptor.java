package com.gitlab.aecsocket.sokol.paper.wrapper;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public record ItemDescriptor(
        SokolPlugin plugin,
        Material material,
        int modelData,
        int damage,
        boolean unbreakable
) implements ItemStack.Factory {
    public static final class Serializer implements TypeSerializer<ItemDescriptor> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        public SokolPlugin plugin() { return plugin; }

        @Override
        public void serialize(Type type, @Nullable ItemDescriptor obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.node("material").set(obj.material.getKey().getKey());
                node.node("model_data").set(obj.modelData);
                node.node("damage").set(obj.damage);
                node.node("unbreakable").set(obj.unbreakable);
            }
        }

        @Override
        public ItemDescriptor deserialize(Type type, ConfigurationNode node) throws SerializationException {
            try {
                return ItemDescriptor.of(plugin,
                        node.node("id").require(String.class),
                        node.node("model_data").getInt(0),
                        node.node("damage").getInt(0),
                        node.node("unbreakable").getBoolean(false)
                );
            } catch (IllegalArgumentException e) {
                throw new SerializationException(node, type, e);
            }
        }
    }

    public static final class Stat extends BasicStat<ItemDescriptor> {
        public Stat(ItemDescriptor defaultValue) {
            super(ItemDescriptor.class, defaultValue, (a, b) -> b);
        }
    }

    public static ItemDescriptor of(SokolPlugin plugin, String id, int modelData, int damage, boolean unbreakable) {
        Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(id));
        if (material == null)
            throw new IllegalArgumentException("Invalid material ID [" + id + "]");
        return new ItemDescriptor(plugin, material, modelData, damage, unbreakable);
    }

    public org.bukkit.inventory.ItemStack createRaw() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(modelData);
        if (meta instanceof Damageable damageable)
            damageable.setDamage(damage);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public PaperItemStack create() {
        return new PaperItemStack(plugin, createRaw());
    }
}
