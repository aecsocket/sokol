package com.gitlab.aecsocket.sokol.paper.wrapper;

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
public record PaperItemStack(
        SokolPlugin plugin,
        org.bukkit.inventory.ItemStack handle
) implements ItemStack {
    public PaperItemStack {
        Validation.assertNot(handle.getType() == Material.AIR, "Item must be non-air");
    }

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
            lore.addAll(add);
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
            lore.addAll(Arrays.asList(add));
            meta.lore(lore);
        });
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
