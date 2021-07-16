package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;

/**
 * Metadata for a slot in a {@link Component}.
 */
public interface Slot {
    /** The tag of a required slot, which must be filled otherwise a tree is not considered complete. */
    String TAG_REQUIRED = "required";
    /** The tag of an internal slot, which is hidden unless all other non-internal slots are empty. */
    String TAG_INTERNAL = "internal";
    /** The tag of a field modifiable slot, which cannot normally be modified. */
    String TAG_FIELD_MODIFIABLE = "field_modifiable";

    /**
     * Gets the localized name of this slot.
     * @param locale The locale to localize for.
     * @return The name.
     */
    net.kyori.adventure.text.Component name(Locale locale);

    /**
     * Gets all of the tags of this slot.
     * @return The tags.
     */
    Collection<String> tags();

    /**
     * Gets if this slot is tagged with a specific tag.
     * @param tag The tag.
     * @return The result.
     */
    boolean tagged(String tag);

    /**
     * Gets the rule that is required to be satisfied if a child component is considered compatible.
     * @return The rule.
     */
    Rule rule();

    /**
     * Gets if a child node is compatible, when provided with the context of the parent node.
     * <p>
     * If the child node is null, it is compatible.
     * @param parent The parent node.
     * @param child The child node.
     * @return The result.
     */
    @Contract("_, null -> true")
    boolean compatible(@Nullable TreeNode parent, @Nullable TreeNode child);

    /**
     * Gets if this slot is tagged with {@link #TAG_REQUIRED}.
     * <p>
     * If tagged, a {@link TreeNode} will not be considered complete unless this slot is filled with a node.
     * @return The result.
     */
    default boolean required() { return tagged(TAG_REQUIRED); }

    /**
     * Gets if this slot is tagged with {@link #TAG_INTERNAL}.
     * <p>
     * If tagged, this slot will not be visible to a user unless all other non-internal slots are empty.
     * @return The result.
     */
    default boolean internal() { return tagged(TAG_INTERNAL); }

    /**
     * Gets if this slot is tagged with {@link #TAG_FIELD_MODIFIABLE}.
     * <p>
     * If tagged, this slot will not normally be modifiable by a user.
     * @return The result.
     */
    default boolean fieldModifiable() { return tagged(TAG_FIELD_MODIFIABLE); }

    /**
     * Gets the key of this slot, as determined by its index in the parent component's {@link Component#slots()}.
     * @return The key.
     */
    String key();

    /**
     * Gets the parent component of this slot, which holds this slot in its {@link Component#slots()}.
     * @return The parent.
     */
    Component parent();
}
