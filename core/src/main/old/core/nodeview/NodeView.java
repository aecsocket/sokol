package com.gitlab.aecsocket.sokol.core.nodeview;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.Component;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.Tree;
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

    protected final Tree<N> tree;
    protected final Options options;
    protected final int amount;
    protected final Consumer<Tree<N>> callback;

    public NodeView(Tree<N> tree, Options options, int amount, Consumer<Tree<N>> callback) {
        this.tree = tree;
        this.options = options;
        this.amount = amount;
        this.callback = callback;
    }

    public Tree<N> tree() { return tree; }
    public Options options() { return options; }
    public int amount() { return amount; }

    public interface Renderer<S extends OffsetSlot, N extends Node.Scoped<N, ?, ? extends Component.Scoped<?, S, ?, ?>, ?>> {
        void renderRoot(Point2 pos);
        void renderRoot(Point2 pos, N parent, S slot, @Nullable N child);
    }

    protected N stripSlots(N node) {
        node = node.asRoot();
        for (var key : new HashSet<>(node.nodeKeys())) {
            node.forceNode(key, null);
        }
        return node;
    }

    public void build(Locale locale, @Nullable Item cursor, Renderer<S, N> renderer) {
        renderer.renderRoot(Point2.ZERO);
        N root = tree.root();
        for (var entry : root.value().slots().entrySet()) {
            var slot = entry.getValue();
            build(locale, cursor, renderer, tree,
                    slot.offset(),
                    root, slot, root.node(entry.getKey()).orElse(null));
        }
    }

    protected void build(Locale locale, @Nullable Item cursor, Renderer<S, N> renderer, Tree<N> tree, Point2 pos, N parent, S slot, @Nullable N node) {
        renderer.renderRoot(pos, parent, slot, node);

        if (node != null) {
            for (var entry : node.value().slots().entrySet()) {
                var childSlot = entry.getValue();
                build(locale, cursor, renderer, tree,
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

        public interface PreModifyParent<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            @Nullable N childNode();
            @Nullable Tree<N> cursor();
        }

        public interface PreModifyChild<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            Tree<N> parent();
            N parentNode();
            @Nullable Tree<N> cursor();
        }

        public interface PreModifyCursor<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            Tree<N> parent();
            N parentNode();
            @Nullable N child();
        }

        public interface SlotModify<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            @Nullable N oldChild();
            @Nullable Tree<N> newChild();
        }

        public interface InsertInto<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            Tree<N> parent();
            N parentNode();
            @Nullable N oldChild();
        }

        public interface RemoveFrom<S extends OffsetSlot, N extends Node.Scoped<N, I, ? extends Component.Scoped<?, S, ?, ?>, ?>, I extends Item.Scoped<I, N>>
                extends Base<S, N, I> {
            Tree<N> parent();
            N parentNode();
            @Nullable Tree<N> newChild();
        }
    }
}
