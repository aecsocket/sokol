package com.gitlab.aecsocket.sokol.paper.wrapper.item;

import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.Damageable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

/**
 * Serializable data class for creating a {@link PaperItemStack}, using a Bukkit {@link org.bukkit.inventory.ItemStack}.
 * @param plugin The platform plugin.
 * @param material The material.
 * @param modelData The model data.
 * @param damage The damage.
 * @param unbreakable If the item cannot be broken, and does not show the durability bar.
 * @param flags The item flags.
 */
public record ItemDescriptor(
        SokolPlugin plugin,
        Material material,
        int modelData,
        int damage,
        boolean unbreakable,
        ItemFlag[] flags
) implements com.gitlab.aecsocket.sokol.core.wrapper.ItemStack.Factory {
    /**
     * Type serializer for a {@link ItemDescriptor}.
     */
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
                node.node("flags").set(obj.flags);
            }
        }

        @Override
        public ItemDescriptor deserialize(Type type, ConfigurationNode node) throws SerializationException {
            try {
                return ItemDescriptor.of(plugin,
                        Serializers.require(node.node("id"), String.class),
                        node.node("model_data").getInt(0),
                        node.node("damage").getInt(0),
                        node.node("unbreakable").getBoolean(false),
                        node.node("flags").get(ItemFlag[].class, new ItemFlag[0])
                );
            } catch (IllegalArgumentException e) {
                throw new SerializationException(node, type, e);
            }
        }
    }

    /**
     * Creates an item descriptor from a material, with flag varargs.
     * @param plugin The platform plugin.
     * @param material The material.
     * @param modelData The model data.
     * @param damage The damage.
     * @param unbreakable If the item cannot be broken, and does not show the durability bar.
     * @param flags The item flags.
     * @return The descriptor.
     */
    public static ItemDescriptor of(SokolPlugin plugin, Material material, int modelData, int damage, boolean unbreakable, ItemFlag... flags) {
        return new ItemDescriptor(plugin, material, modelData, damage, unbreakable, flags);
    }

    /**
     * Creates an item descriptor from a string ID instead of a material.
     * @param plugin The platform plugin.
     * @param id The ID.
     * @param modelData The model data.
     * @param damage The damage.
     * @param unbreakable If the item cannot be broken, and does not show the durability bar.
     * @param flags The item flags.
     * @return The descriptor.
     */
    public static ItemDescriptor of(SokolPlugin plugin, String id, int modelData, int damage, boolean unbreakable, ItemFlag... flags) {
        Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(id));
        if (material == null)
            throw new IllegalArgumentException("Invalid material ID [" + id + "]");
        return new ItemDescriptor(plugin, material, modelData, damage, unbreakable, flags);
    }

    /**
     * Applies this descriptor to an existing item.
     * @param item The item.
     * @return The item passed.
     */
    public ItemStack apply(ItemStack item) {
        item.setType(material);
        return PaperUtils.modify(item, meta -> {
            meta.addItemFlags(flags);
            meta.setCustomModelData(modelData);
            if (meta instanceof Damageable damageable)
                damageable.setDamage(damage);
        });
    }

    /**
     * Creates a Bukkit item stack.
     * @return The item stack.
     */
    public ItemStack createRaw() {
        return apply(new ItemStack(material));
    }

    @Override
    public PaperItemStack create() {
        return new PaperItemStack(plugin, createRaw());
    }
}
