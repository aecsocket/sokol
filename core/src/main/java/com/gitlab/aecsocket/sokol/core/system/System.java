package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * A system which is applicable to a {@link Component}, and creates {@link System.Instance}s.
 */
public interface System extends LoadProvider {
    /**
     * A descriptor of a system instance, used in getting a system from a node.
     * @param <S> The system instance type.
     */
    record Key<S extends System.Instance>(String id, TypeToken<S> instanceType) {
        public Key(String id, Class<S> instanceType) {
            this(id, TypeToken.get(instanceType));
        }
    }

    /**
     * A system instance which is applicable to a {@link TreeNode}, and interacts with the
     * node by intercepting events.
     */
    interface Instance {
        /**
         * Gets the parent node that this system is a system of.
         * @return The parent.
         */
        TreeNode parent();

        /**
         * Gets the base system of this instance.
         * @return The base system.
         */
        System base();

        /**
         * Gets the platform that this system uses.
         * @return The platform.
         */
        SokolPlatform platform();

        /**
         * Sets this system up using the parent.
         * <p>
         * This can cover a multitude of functions, such as:
         * <ul>
         *     <li>registering event listeners using the parent's {@link TreeNode#events()}</li>
         *     <li>setting fields on this system instance, which are obtained from the parent</li>
         *     <li>adding custom stats built at system-build time</li>
         * </ul>
         * @param stats The existing stats which can be combined with.
         */
        default void build(StatLists stats) {}

        default void runAction(SchedulerSystem<?>.Instance scheduler, ItemUser user, ItemSlot slot, String key) {
            parent().stats().<Long>val(key + "_delay").ifPresent(scheduler::delay);
        }
    }

    /**
     * Gets this system's ID.
     * @return The ID.
     */
    String id();

    /**
     * Creates an instance of this system, applicable on {@link TreeNode}s.
     * @param node The parent tree node.
     * @return The instance.
     */
    Instance create(TreeNode node);

    /**
     * Loads this system instance after all proper serializer setup has been complete.
     * @param cfg The configuration.
     * @throws SerializationException If serialization failed.
     */
    default void loadSelf(ConfigurationNode cfg) throws SerializationException {}
}
