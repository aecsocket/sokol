package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * A system which is applicable to a {@link Component}, and creates {@link System.Instance}s.
 */
public interface System {
    /**
     * A system instance which is applicable to a {@link TreeNode}, and interacts with the
     * node by intercepting events.
     */
    interface Instance {
        /**
         * Gets the parent node that this system is a system of.
         * @return The parent.
         */
        @NotNull TreeNode parent();

        /**
         * Gets the base system of this instance.
         * @return The base system.
         */
        @NotNull System base();

        /**
         * Gets the platform that this system uses.
         * @return The platform.
         */
        @NotNull SokolPlatform platform();

        /**
         * Sets this system up using the parent.
         * <p>
         * This can cover a multitude of functions, such as:
         * <ul>
         *     <li>registering event listeners using the parent's {@link TreeNode#events()}</li>
         *     <li>setting fields on this system instance, which are obtained from the parent</li>
         * </ul>
         */
        default void build() {}
    }

    /**
     * Gets this system's ID.
     * @return The ID.
     */
    @NotNull String id();

    /**
     * Gets the stat types that this system defines, used for deserialization.
     * @return The stat types.
     */
    default @NotNull Map<String, Stat<?>> statTypes() { return Collections.emptyMap(); }

    /**
     * Gets the rule types that this system defines, used for deserialization.
     * @return The rule types.
     */
    default @NotNull Map<String, Class<? extends Rule>> ruleTypes() { return Collections.emptyMap(); }

    /**
     * Creates an instance of this system, applicable on {@link TreeNode}s.
     * @param node The parent tree node.
     * @return The instance.
     */
    @NotNull Instance create(TreeNode node);
}
