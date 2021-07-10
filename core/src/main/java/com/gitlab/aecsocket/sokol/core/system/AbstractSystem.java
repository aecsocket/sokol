package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import net.kyori.adventure.text.Component;

import java.util.Locale;

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

        protected Component localize(Localizer localizer, Locale locale, String key, Object... args) {
            return localizer.localize(locale, "system." + base().id() + "." + key, args);
        }

        @Override public String toString() {
            return "<%s>".formatted(base().id());
        }
    }

    @Override public String toString() { return "(%s)".formatted(id()); }
}
