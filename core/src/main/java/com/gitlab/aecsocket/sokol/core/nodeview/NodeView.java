package com.gitlab.aecsocket.sokol.core.nodeview;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.Component;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.event.UserEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractNode;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashSet;
import java.util.Locale;
import java.util.function.Consumer;

public class NodeView<S extends OffsetSlot, N extends AbstractNode<N, ?, ? extends Component.Scoped<?, S, ?, ?>, ?>> {
    @ConfigSerializable
    public record Options(
            boolean modifiable,
            boolean limited
    ) {
        public static final Options DEFAULT = new Options();

        public Options() {
            this(true, false);
        }
    }

    public static final String TAG_MODIFIABLE = "modifiable";

    public static boolean modifiable(Slot slot) {
        return slot.tagged(TAG_MODIFIABLE);
    }

    protected final N root;
    protected final Options options;
    protected final int amount;
    protected final Consumer<N> callback;

    public NodeView(N root, Options options, int amount, Consumer<N> callback) {
        this.root = root;
        this.options = options;
        this.amount = amount;
        this.callback = callback;
    }

    public N root() { return root; }
    public Options options() { return options; }
    public int amount() { return amount; }

    public interface Renderer<S extends OffsetSlot, N extends Node.Scoped<N, ?, ? extends Component.Scoped<?, S, ?, ?>, ?>> {
        void render(Point2 pos, N root);
        void render(Point2 pos, N parent, S slot);
        void render(Point2 pos, N parent, S slot, N child);
    }

    protected N stripSlots(N node) {
        node = node.copy();
        for (var key : new HashSet<>(node.nodeKeys())) {
            node.unsafeNode(key, null);
        }
        return node;
    }

    public void build(Locale locale, @Nullable Item cursor, Renderer<S, N> renderer) {
        renderer.render(Point2.ZERO, root);
        for (var entry : root.value().slots().entrySet()) {
            var slot = entry.getValue();
            build(locale, cursor, renderer,
                    slot.offset(),
                    root, slot, root.node(entry.getKey()).orElse(null));
        }
    }

    protected void build(Locale locale, @Nullable Item cursor, Renderer<S, N> renderer, Point2 pos, N parent, S slot, @Nullable N node) {
        if (node == null)
            renderer.render(pos, parent, slot);
        else
            renderer.render(pos, parent, slot, node);

        if (node != null) {
            for (var entry : node.value().slots().entrySet()) {
                var childSlot = entry.getValue();
                build(locale, cursor, renderer,
                        new Point2(pos.x() + childSlot.offset().x(), pos.y() + childSlot.offset().y()),
                        node, childSlot, node.node(entry.getKey()).orElse(null));
            }
        }
    }

    public static final class Events {
        private Events() {}


        public interface Base<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends UserEvent<N>, Cancellable {
            S slot();
        }

        public interface PreModify<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            @Nullable N cursor();
        }

        public interface SlotModify<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            @Nullable N oldChild();
            @Nullable N newChild();
        }

        public interface InsertInto<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            N parent();
            @Nullable N oldChild();
        }

        public interface RemoveFrom<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            N parent();
            @Nullable N newChild();
        }
    }
}
