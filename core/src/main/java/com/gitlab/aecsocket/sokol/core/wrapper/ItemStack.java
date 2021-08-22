package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.OptionalDouble;

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
     * Gets this item stack as a node, if it is a node.
     * @return The node.
     */
    Optional<? extends TreeNode> node();

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

    /**
     * Gets the durability bar percentage of this item stack, if it supports a durability bar.
     * @return The percentage full the bar is, or an empty optional if this item does not support
     *         a durability bar.
     */
    OptionalDouble durability();

    /**
     * Sets the durability bar percentage of this item stack.
     * <p>
     * This method ensures that the durability bar appears, however the durability will not
     * drop enough to break the item, nor be high enough to hide the bar.
     * @param percent The percentage full the bar is.
     */
    void durability(double percent);

    /**
     * Sets the durability of this item to its maximum.
     * <p>
     * This is separate from {@link #durability(double)}, as this will ensure that the bar
     * will be hidden.
     */
    void maxDurability();

    /**
     * Hides any item update to players.
     * @return An item stack with the new property.
     */
    ItemStack hideUpdate();

    /**
     * Shows any item update to players.
     * @return An item stack with the new property.
     */
    ItemStack showUpdate();

    /**
     * Gets if any updates to this item are hidden to players.
     * @return The result.
     */
    boolean updateHidden();
}
