package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * A node in a component tree.
 */
public interface TreeNode {
    /**
     * A scoped version of a node.
     * @param <N> The type of this node.
     * @param <C> The component type.
     * @param <S> The slot type.
     * @param <B> The base system type.
     * @param <Y> The system instance type.
     */
    interface Scoped<N extends Scoped<N, C, S, B, Y>, C extends Component.Scoped<C, S, B>, S extends Slot, B extends System, Y extends System.Instance>
            extends TreeNode {
        @Override C value();
        @Override EventDispatcher<TreeEvent> events();

        @Override Map<String, N> children();

        @Override Optional<N> child(String key);

        /**
         * Sets a child of this node.
         * @param key The key of the slot.
         * @param child The child node, or null if the slot should be emptied.
         * @throws IllegalArgumentException If the slot key is invalid, or the child is incompatible with the slot.
         */
        void child(String key, N child) throws IllegalArgumentException;

        @Override Optional<N> node(String... path);

        @Override Map<String, ChildSlot<S, N>> slotChildren();

        @Override Map<String, Y> systems();

        /**
         * Sets a system instance on this node.
         * @param system The system.
         * @throws IllegalArgumentException If the system's {@link System.Instance#base()} does not exist on
         * the {@link #value()}'s {@link Component#baseSystems()}.
         */
        void system(Y system) throws IllegalArgumentException;

        @Override N build();

        @Override N combine(TreeNode node, boolean limited);

        /**
         * Recursively visits this node and its children, applying a function on nodes.
         * @param visitor The visitor function.
         * @param path The path to start from. This can be left blank.
         * @see #visitNodes(NodeVisitor, String...)
         */
        void visitNodesScoped(NodeVisitor<N> visitor, String... path);

        /**
         * Recursively visits this node's slots, applying a function on slots.
         * @param visitor The visitor function.
         * @param path The path to start from. This can be left blank.
         * @see #visitNodes(NodeVisitor, String...)
         */
        void visitSlotsScoped(SlotVisitor<N, S> visitor, String... path);

        @Override Optional<N> parent();
        @Override Optional<S> slot();

        @Override N root();
        @Override N asRoot();
    }

    /**
     * A pair of a slot and optional child.
     * @param <S> The slot.
     * @param <N> The optional child.
     */
    record ChildSlot<S extends Slot, N extends TreeNode>(S slot, Optional<N> child) {}

    /**
     * A function which accepts a node.
     * @param <N> The node type.
     */
    @FunctionalInterface
    interface NodeVisitor<N extends TreeNode> {
        void visit(N node, String... path);
    }

    /**
     * A function which accepts a slot, which may or may not have a child.
     * @param <N> The node type.
     * @param <S> The slot type.
     */
    @FunctionalInterface
    interface SlotVisitor<N extends TreeNode, S extends Slot> {
        void visit(N parent, S slot, @Nullable N child, String... path);
    }

    /**
     * Gets the component that this node refers to.
     * @return The component.
     */
    Component value();

    /**
     * Gets this node's event dispatcher.
     * <p>
     * Any parent and child of this node must have the same event dispatcher reference.
     * @return The event dispatcher.
     */
    EventDispatcher<TreeEvent> events();

    /**
     * Gets this node's stat map.
     * <p>
     * Any parent and child of this node must have the same stat map reference.
     * @return The stat map.
     */
    StatMap stats();

    /**
     * Gets if this node is complete.
     * <p>
     * Any parent and child of this node must have the same completeness status.
     * @return The completeness.
     */
    boolean complete();

    /**
     * Gets a map of all children on this node, mapped to the slot key.
     * <p>
     * Each key must also exist on this node's {@link #value()}'s {@link Component#slots()}.
     * <p>
     * Each value must be non-null.
     * @return The map of children.
     */
    Map<String, ? extends TreeNode> children();

    /**
     * Gets a child of this node.
     * @param key The key of the slot to get the child from.
     * @return An Optional of the result.
     */
    Optional<? extends TreeNode> child(String key);

    /**
     * Gets a child of this node, or child nodes, according to the path.
     * @param path The path to the child.
     * @return An Optional of the result.
     */
    Optional<? extends TreeNode> node(String... path);

    /**
     * Gets a map of all slots on the {@link #value()}, and the corresponding child on this node.
     * <p>
     * If this node has no child for a specified key, then the resulting {@link ChildSlot} will have an
     * empty optional for the child field.
     * @return The map.
     */
    Map<String, ? extends ChildSlot<?, ?>> slotChildren();

    /**
     * Gets a map of all system instances on this node, mapped to the system's ID.
     * <p>
     * Each key must also exist on this node's {@link #value()}'s {@link Component#baseSystems()}.
     * <p>
     * Each value must be non-null.
     * @return The map of systems.
     */
    Map<String, ? extends System.Instance> systems();

    Optional<? extends System.Instance> system(String id);

    <S extends System.Instance> Optional<S> system(System.Key<S> key);

    <S extends System.Instance> Optional<S> system(Class<S> type);

    /**
     * Completes the node tree by:
     * <ul>
     *     <li>correcting all parent references in this node and child nodes</li>
     *     <li>re-registering all event listeners on systems in this tree</li>
     *     <li>re-calculating the tree's stats</li>
     *     <li>correcting the {@code complete} stats</li>
     * </ul>
     * @return This instance.
     */
    TreeNode build();

    /**
     * Places a child node into the first empty slot of this (parent) node.
     * @param node The child node.
     * @param limited If the slot must be {@link Slot#fieldModifiable()} to be valid.
     * @return The parent tree node that the child was placed into.
     */
    TreeNode combine(TreeNode node, boolean limited);

    /**
     * Recursively visits this node and its children, applying a function on nodes.
     * @param visitor The visitor function.
     * @param path The path to start from. This can be left blank.
     */
    void visitNodes(NodeVisitor<TreeNode> visitor, String... path);

    /**
     * Recursively visits this node's slots, applying a function on slots.
     * @param visitor The visitor function.
     * @param path The path to start from. This can be left blank.
     */
    void visitSlots(SlotVisitor<TreeNode, Slot> visitor, String... path);

    /**
     * Gets this node's parent node.
     * @return An Optional of the result.
     */
    Optional<? extends TreeNode> parent();

    /**
     * Gets this node's parent slot.
     * @return An Optional of the result.
     */
    Optional<? extends Slot> slot();

    /**
     * Gets the path to this slot, from the root.
     * <p>
     * If this node is the root, returns an empty array.
     * @return The path.
     */
    String[] path();

    /**
     * Gets if this node is the root of its tree.
     * @return The result.
     */
    default boolean isRoot() { return slot().isEmpty(); }

    /**
     * Gets the root of this tree.
     * <p>
     * If this node is the root, returns this node.
     * @return The root.
     */
    TreeNode root();

    /**
     * Copies this node and sets the copy as the root.
     * <p>
     * On the copy, parent nodes will be discarded, child nodes will be kept,
     * and fields will be recalculated according to {@link #build()}.
     * @return The copied node, as the root.
     */
    TreeNode asRoot();
}
