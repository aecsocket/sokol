package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class PersistenceManager {
    private final class GetTreeNodeDataType implements PersistentDataType<PersistentDataContainer, PaperTreeNode> {
        @Override public @NotNull Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
        @Override public @NotNull Class<PaperTreeNode> getComplexType() { return PaperTreeNode.class; }

        @Override
        public @NotNull PersistentDataContainer toPrimitive(@NotNull PaperTreeNode a, @NotNull PersistentDataAdapterContext b) {
            throw new UnsupportedOperationException("Use the Set data type");
        }

        private PaperTreeNode tree(PaperTreeNode parent, PersistentDataContainer data) {
            String id = data.get(plugin.key("id"), PersistentDataType.STRING);
            PaperComponent component = plugin.component(id);
            if (id == null || component == null)
                throw new IllegalArgumentException("No component with ID [" + id + "]");

            PaperTreeNode tree = parent == null ? new PaperTreeNode(component) : new PaperTreeNode(component, parent);

            try {
                PersistentDataContainer slots = data.get(plugin.key("slots"), PersistentDataType.TAG_CONTAINER);
                if (slots != null) {
                    for (var key : slots.getKeys()) {
                        String slot = key.getKey();
                        try {
                            tree.child(slot, tree(tree, slots.get(key, PersistentDataType.TAG_CONTAINER)));
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
                        tree.system(system.load(tree, systems.get(key, PersistentDataType.TAG_CONTAINER)));
                    }
                }

                return tree;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("(" + id + " " + e.getMessage() + ")", e);
            }
        }

        @Override
        public @NotNull PaperTreeNode fromPrimitive(@NotNull PersistentDataContainer data, @NotNull PersistentDataAdapterContext ctx) {
            return tree(null, data).build();
        }
    }

    private final class SetTreeNodeDataType implements PersistentDataType<PersistentDataContainer, TreeNode> {
        @Override public @NotNull Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
        @Override public @NotNull Class<TreeNode> getComplexType() { return TreeNode.class; }

        @Override
        public @NotNull PersistentDataContainer toPrimitive(@NotNull TreeNode obj, @NotNull PersistentDataAdapterContext ctx) {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            Component component = obj.value();
            data.set(plugin.key("id"), PersistentDataType.STRING, component.id());

            PersistentDataContainer slots = ctx.newPersistentDataContainer();
            for (var entry : obj.children().entrySet()) {
                slots.set(plugin.key(entry.getKey()), PersistentDataType.TAG_CONTAINER, toPrimitive(entry.getValue(), ctx));
            }
            data.set(plugin.key("slots"), PersistentDataType.TAG_CONTAINER, slots);

            PersistentDataContainer systems = ctx.newPersistentDataContainer();
            for (var entry : obj.systems().entrySet()) {
                if (entry.getValue() instanceof PaperSystem.Instance paperSystem) {
                    PersistentDataContainer system = paperSystem.save(ctx);
                    if (system != null)
                        systems.set(plugin.key(entry.getKey()), PersistentDataType.TAG_CONTAINER, system);
                }
            }
            data.set(plugin.key("systems"), PersistentDataType.TAG_CONTAINER, systems);

            return data;
        }

        @Override
        public @NotNull TreeNode fromPrimitive(@NotNull PersistentDataContainer persistentDataContainer, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
            throw new UnsupportedOperationException("Use the Get data type");
        }
    }

    private final SokolPlugin plugin;
    private final NamespacedKey key;
    private final GetTreeNodeDataType getDataType;
    private final SetTreeNodeDataType setDataType;

    PersistenceManager(SokolPlugin plugin) {
        this.plugin = plugin;
        key = plugin.key("tree");
        getDataType = new GetTreeNodeDataType();
        setDataType = new SetTreeNodeDataType();
    }

    public SokolPlugin plugin() { return plugin; }

    public PersistentDataContainer save(PersistentDataContainer data, TreeNode tree) {
        data.set(key, setDataType, tree);
        return data;
    }

    public boolean isTree(PersistentDataContainer data) {
        return data.getKeys().contains(key);
    }

    public PaperTreeNode load(PersistentDataContainer data) {
        try {
            return data.get(key, getDataType);
        } catch (IllegalArgumentException e) {
            plugin.log(Logging.Level.WARNING, e, "Could not load tree node from item stack");
            return null;
        }
    }

    private <T> T use(ItemStack item, Function<PersistentDataContainer, T> dataFunction, T defaultValue) {
        if (item == null)
            return defaultValue;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return defaultValue;
        return dataFunction.apply(meta.getPersistentDataContainer());
    }

    public boolean isTree(ItemStack item) {
        return use(item, this::isTree, false);
    }

    public PaperTreeNode load(ItemStack item) {
        return use(item, this::load, null);
    }
}
