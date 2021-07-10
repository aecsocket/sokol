package com.gitlab.aecsocket.sokol.paper.wrapper;

import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import io.leangen.geantyref.TypeToken;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.Damageable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

/**
 * Serializable data class for creating a {@link PaperItemStack}, using a Bukkit {@link org.bukkit.inventory.ItemStack}.
 */
public record ItemDescriptor(
        SokolPlugin plugin,
        Material material,
        int modelData,
        int damage,
        boolean unbreakable
) implements ItemStack.Factory {
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
            }
        }

        @Override
        public ItemDescriptor deserialize(Type type, ConfigurationNode node) throws SerializationException {
            try {
                return ItemDescriptor.of(plugin,
                        Serializers.require(node.node("id"), String.class),
                        node.node("model_data").getInt(0),
                        node.node("damage").getInt(0),
                        node.node("unbreakable").getBoolean(false)
                );
            } catch (IllegalArgumentException e) {
                throw new SerializationException(node, type, e);
            }
        }
    }

    /**
     * A stat type which stores an item descriptor.
     */
    public static final class Stat extends BasicStat<ItemDescriptor> {
        public Stat(@Nullable ItemDescriptor defaultValue) {
            super(new TypeToken<ItemDescriptor>() {}, defaultValue, (a, b) -> b, i -> i);
        }
    }

    /**
     * Creates an item descriptor from a string ID instead of a material.
     * @param plugin The platform plugin.
     * @param id The ID.
     * @param modelData The model data.
     * @param damage The damage.
     * @param unbreakable If the item is unbreakable or not.
     * @return The descriptor.
     */
    public static ItemDescriptor of(SokolPlugin plugin, String id, int modelData, int damage, boolean unbreakable) {
        Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(id));
        if (material == null)
            throw new IllegalArgumentException("Invalid material ID [" + id + "]");
        return new ItemDescriptor(plugin, material, modelData, damage, unbreakable);
    }

    /**
     * Creates a Bukkit item stack.
     * @return The item stack.
     */
    public org.bukkit.inventory.ItemStack createRaw() {
        return PaperUtils.modify(new org.bukkit.inventory.ItemStack(material), meta -> {
            meta.addItemFlags(ItemFlag.values());
            meta.setCustomModelData(modelData);
            if (meta instanceof Damageable damageable)
                damageable.setDamage(damage);
        });
    }

    @Override
    public PaperItemStack create() {
        return new PaperItemStack(plugin, createRaw());
    }
}
