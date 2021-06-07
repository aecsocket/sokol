package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class ItemManager {
    private final class TreeNodeDataType implements PersistentDataType<PersistentDataContainer, PaperTreeNode> {
        @Override public @NotNull Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
        @Override public @NotNull Class<PaperTreeNode> getComplexType() { return PaperTreeNode.class; }

        @Override
        public @NotNull PersistentDataContainer toPrimitive(@NotNull PaperTreeNode obj, @NotNull PersistentDataAdapterContext ctx) {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            PaperComponent component = obj.value();
            data.set(plugin.key("id"), PersistentDataType.STRING, component.id());

            PersistentDataContainer slots = ctx.newPersistentDataContainer();
            for (var entry : obj.children().entrySet()) {
                slots.set(plugin.key(entry.getKey()), PersistentDataType.TAG_CONTAINER, toPrimitive(entry.getValue(), ctx));
            }
            data.set(plugin.key("slots"), PersistentDataType.TAG_CONTAINER, slots);

            PersistentDataContainer systems = ctx.newPersistentDataContainer();
            for (var entry : obj.systems().entrySet()) {
                systems.set(plugin.key(entry.getKey()), PersistentDataType.TAG_CONTAINER, entry.getValue().save(ctx));
            }
            data.set(plugin.key("systems"), PersistentDataType.TAG_CONTAINER, systems);

            return data;
        }

        @Override
        public @NotNull PaperTreeNode fromPrimitive(@NotNull PersistentDataContainer data, @NotNull PersistentDataAdapterContext ctx) {
            String id = data.get(plugin.key("id"), PersistentDataType.STRING);
            PaperComponent component = plugin.components().get(id);
            if (id == null || component == null)
                throw new IllegalArgumentException("No component with ID [" + id + "]");

            PaperTreeNode tree = new PaperTreeNode(component);

            try {
                PersistentDataContainer slots = data.get(plugin.key("slots"), PersistentDataType.TAG_CONTAINER);
                if (slots != null) {
                    for (var key : slots.getKeys()) {
                        String slot = key.getKey();
                        try {
                            tree.child(slot, slots.get(key, this));
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("[" + slot + "]", e);
                        }
                    }
                }

                PersistentDataContainer systems = data.get(plugin.key("systems"), PersistentDataType.TAG_CONTAINER);
                if (systems != null) {
                    for (var key : systems.getKeys()) {
                        String systemId = key.getKey();
                        PaperSystem system = component.baseSystems().get(systemId);
                        if (system == null)
                            throw new IllegalArgumentException("No system with ID [" + systemId + "]");
                        tree.system(system.load(systems.get(key, PersistentDataType.TAG_CONTAINER)));
                    }
                }

                return tree;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Component ID [" + id + "]", e);
            }
        }
    }

    private final SokolPlugin plugin;
    private final NamespacedKey key;
    private final TreeNodeDataType dataType;

    public ItemManager(SokolPlugin plugin) {
        this.plugin = plugin;
        key = plugin.key("tree");
        dataType = new TreeNodeDataType();
    }

    public SokolPlugin plugin() { return plugin; }

    public ItemStack save(ItemStack stack, PaperTreeNode tree) {
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(key, dataType, tree);
        stack.setItemMeta(meta);
        return stack;
    }

    public PaperTreeNode load(ItemStack stack) {
        PersistentDataContainer data = stack.getItemMeta().getPersistentDataContainer();
        try {
            return data.get(key, dataType);
        } catch (IllegalArgumentException e) {
            plugin.log(Logging.Level.WARNING, e, "Could not load tree node from item stack");
            return null;
        }
    }
}
