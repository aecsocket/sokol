package com.gitlab.aecsocket.sokol.core.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;

public abstract class SchedulerSystem extends AbstractSystem {
    /** The system ID. */
    public static final String ID = "scheduler";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public SchedulerSystem(int listenerPriority) {
        super(listenerPriority);
    }

    /**
     * See {@link SchedulerSystem}.
     */
    public static abstract class Instance extends AbstractSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override
        public void build() {
            parent.events().register(ItemTreeEvent.Holding.class, this::event);
            // TODO
        }

        private void event(ItemTreeEvent.Holding event) {

        }
    }

    @Override public String id() { return ID; }
}
