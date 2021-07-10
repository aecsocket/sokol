package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import net.kyori.adventure.text.Component;

import java.util.Collection;

/**
 * An item which resides in a single slot.
 */
public interface ItemStack {
    /**
     * An item stack creator.
     */
    @FunctionalInterface
    interface Factory {
        ItemStack create();
    }

    /**
     * Gets how many items are currently in this stack.
     * @return The amount.
     */
    int amount();

    /**
     * Sets the amount of items currently in this stack.
     * @param amount The amount.
     * @return An item stack with the new stack amount.
     */
    ItemStack amount(int amount);

    /**
     * Adds an amount of items to this stack.
     * @param amount The amount to add.
     * @return An item stack with the new stack amount.
     * @see #subtract()
     */
    default ItemStack add(int amount) { return amount(amount() + amount); }

    /**
     * Subtracts a single item from this stack.
     * @return An item stack with the new stack amount.
     * @see #add(int)
     */
    default ItemStack subtract() { return add(-1); }

    /**
     * Saves a specified tree node into this item.
     * @param node The node to save
     */
    void save(TreeNode node);

    /**
     * Sets the name of this item stack.
     * @param name The name.
     */
    void name(Component name);

    /**
     * Adds a section of lore to this item stack.
     * <p>
     * Implementations may wish to separate calls with their own line separator.
     * @param add The lore to add.
     */
    void addLore(Collection<Component> add);

    /**
     * Adds a section of lore to this item stack.
     * <p>
     * Implementations may wish to separate calls with their own line separator.
     * @param add The lore to add.
     */
    void addLore(Component... add);
}
