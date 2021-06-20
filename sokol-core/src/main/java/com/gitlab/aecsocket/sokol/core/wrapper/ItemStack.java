package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import net.kyori.adventure.text.Component;

import java.util.Collection;

public interface ItemStack {
    interface Factory {
        ItemStack create();
    }

    int amount();
    ItemStack amount(int amount);
    default ItemStack add(int amount) { return amount(amount() + amount); }
    default ItemStack subtract() { return add(-1); }

    void save(TreeNode node);

    void name(Component name);
    void addLore(Collection<Component> add);
    void addLore(Component... add);
}
