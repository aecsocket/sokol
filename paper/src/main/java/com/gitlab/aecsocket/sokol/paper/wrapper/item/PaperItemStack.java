package com.gitlab.aecsocket.sokol.paper.wrapper.item;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.Validation;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.*;

/**
 * Wrapper around an item stack which uses an underlying Bukkit {@link org.bukkit.inventory.ItemStack}.
 */
public final class PaperItemStack implements ItemStack {
    private final SokolPlugin plugin;
    private final org.bukkit.inventory.ItemStack handle;
    private boolean hidden;

    public PaperItemStack(SokolPlugin plugin, org.bukkit.inventory.ItemStack handle) {
        Validation.assertNot(handle.getType() == Material.AIR, "Item must be non-air");
        this.plugin = plugin;
        this.handle = handle;
        hidden = plugin.packetListener().updatesHidden(handle);
    }

    public SokolPlugin plugin() { return plugin; }
    public org.bukkit.inventory.ItemStack handle() { return handle; }

    @Override public int amount() { return handle.getAmount(); }
    @Override public PaperItemStack amount(int amount) { handle.setAmount(amount); return this; }

    @Override
    public void save(TreeNode node) {
        PaperUtils.modify(handle, meta -> plugin.persistenceManager().save(meta.getPersistentDataContainer(), node));
    }

    @Override
    public void name(Component name) {
        PaperUtils.modify(handle, meta -> meta.displayName(Components.BLANK.append(name)));
    }

    @Override
    public void addLore(Collection<Component> add) {
        PaperUtils.modify(handle, meta -> {
            List<Component> lore = meta.lore();
            if (lore == null)
                lore = new ArrayList<>();
            else
                lore.add(Component.empty());
            for (Component component : add)
                lore.add(Components.BLANK.append(component));
            meta.lore(lore);
        });
    }

    @Override
    public void addLore(Component... add) {
        PaperUtils.modify(handle, meta -> {
            List<Component> lore = meta.lore();
            if (lore == null)
                lore = new ArrayList<>();
            else
                lore.add(Component.empty());
            for (Component component : add)
                lore.add(Components.BLANK.append(component));
            meta.lore(lore);
        });
    }

    @Override
    public ItemStack hideUpdate() {
        if (!hidden) {
            plugin.packetListener().hideUpdate(handle);
            hidden = true;
        }
        return this;
    }

    @Override
    public ItemStack showUpdate() {
        if (hidden) {
            plugin.packetListener().showUpdate(handle);
            hidden = false;
        }
        return this;
    }

    @Override
    public boolean updateHidden() {
        return hidden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaperItemStack that = (PaperItemStack) o;
        return handle.equals(that.handle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handle);
    }
}
