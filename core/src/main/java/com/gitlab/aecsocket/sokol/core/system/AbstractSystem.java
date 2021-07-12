package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * An abstract system with some methods implemented.
 */
public abstract class AbstractSystem implements System {
    /**
     * An abstract system instance.
     */
    public static abstract class Instance implements System.Instance {
        protected final TreeNode parent;

        public Instance(TreeNode parent) {
            this.parent = parent;
        }

        @Override public TreeNode parent() { return parent; }

        protected String lck(String key) {
            return "system." + base().id() + "." + key;
        }

        @Override public String toString() {
            return "<%s>".formatted(base().id());
        }
    }

    @Override public String toString() { return "(%s)".formatted(id()); }
}
