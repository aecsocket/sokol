package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.Map;

/**
 * A system which is applicable to a {@link Component}, and creates {@link System.Instance}s.
 */
public interface System {
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
    }

    /**
     * Gets this system's ID.
     * @return The ID.
     */
    String id();

    /**
     * Gets the stat types that this system defines, used for deserialization.
     * @return The stat types.
     */
    default Map<String, Stat<?>> statTypes() { return Collections.emptyMap(); }

    /**
     * Gets the rule types that this system defines, used for deserialization.
     * @return The rule types.
     */
    default Map<String, Class<? extends Rule>> ruleTypes() { return Collections.emptyMap(); }

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
