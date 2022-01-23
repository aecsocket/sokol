package old.feature;

import old.FeatureType;
import old.SokolPlugin;
import old.event.PaperItemEvent;
import old.impl.PaperFeature;
import old.impl.PaperFeatureInstance;
import old.impl.PaperNode;
import old.impl.PaperNodeView;
import old.stat.SoundsStat;
import old.wrapper.PaperItem;
import old.wrapper.slot.PaperItemSlot;
import old.wrapper.user.PaperUser;
import old.wrapper.user.PlayerUser;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.incendo.interfaces.core.view.InterfaceView;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.type.ChestInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Map;

import com.github.aecsocket.sokol.core.event.ItemEvent;
import com.github.aecsocket.sokol.core.feature.NodeViewFeature;
import com.github.aecsocket.sokol.core.nodeview.NodeView;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.stat.StatTypes;

import static old.stat.SoundsStat.*;

public final class PaperNodeViewFeature extends NodeViewFeature<PaperNodeViewFeature.Instance, PaperNode, PaperItem>
        implements PaperFeature<PaperNodeViewFeature.Instance> {
    public static final SoundsStat STAT_SLOT_INSERT = soundsStat(KEY_SLOT_INSERT_SOUND);
    public static final SoundsStat STAT_SLOT_REMOVE = soundsStat(KEY_SLOT_REMOVE_SOUND);
    public static final SoundsStat STAT_SLOT_COMBINE = soundsStat(KEY_SLOT_COMBINE_SOUND);

    public static final StatTypes STAT_TYPES = StatTypes.types(
            STAT_SLOT_INSERT,
            STAT_SLOT_REMOVE,
            STAT_SLOT_COMBINE
    );
    public static final Map<String, Class<? extends NodeRule>> RULE_TYPES = NodeRule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new PaperNodeViewFeature(platform,
            config.node("listener_priority").getInt(PRIORITY_HIGH),
            config.node("options").get(NodeView.Options.class, NodeView.Options.DEFAULT),
            config.node("combine").get(NodeView.Options.class, NodeView.Options.DEFAULT)
    ));

    private final SokolPlugin platform;

    public PaperNodeViewFeature(SokolPlugin platform, int listenerPriority, NodeView.Options options, NodeView.Options combine) {
        super(listenerPriority, options, combine);
        this.platform = platform;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {}

    @Override public SokolPlugin platform() { return platform; }

    @Override
    public Instance load(PaperNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperNode node, Type type, ConfigurationNode config) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperNode node, PersistentDataContainer pdc) throws IllegalArgumentException {
        return new Instance(node);
    }

    public final class Instance extends NodeViewFeature<Instance, PaperNode, PaperItem>.Instance implements PaperFeatureInstance {
        public Instance(PaperNode parent) {
            super(parent);
        }

        @Override
        protected boolean shouldCancel(ItemEvent.SlotClick<PaperNode, PaperItem> event) {
            if (!(event instanceof PaperItemEvent.SlotClick paperEvent))
                return false;
            if (paperEvent.handle().getInventory().getHolder() instanceof InterfaceView<?, ?> ifView) {
                for (var transform : ifView.backing().transformations()) {
                    if (transform.transform() instanceof PaperNodeView<?> nodeTreeTf) {
                        if (paperEvent.handle().getSlot() == nodeTreeTf.clickedSlot()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        protected boolean shouldCancel(ItemEvent.SlotDrag<PaperNode, PaperItem> event) {
            if (!(event instanceof PaperItemEvent.SlotDrag paperEvent))
                return false;
            if (paperEvent.handle().getInventory().getHolder() instanceof InterfaceView<?, ?> ifView) {
                for (var transform : ifView.backing().transformations()) {
                    if (transform.transform() instanceof PaperNodeView<?> nodeTreeTf) {
                        if (paperEvent.handle().getView().convertSlot(event.rawSlot()) == nodeTreeTf.clickedSlot()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        protected Events.@Nullable CombineChildOntoN createCombineChildOntoN(ItemEvent.SlotClick<PaperNode, PaperItem> event, PaperNode parent, PaperNode child) {
            return event instanceof PaperItemEvent.SlotClick paper
                    ? new Events.CombineChildOntoN(parent, paper.user(), paper.slot(), paper.item(), this, child)
                    : null;
        }

        @Override
        protected Events.@Nullable CombineNOntoParent createCombineNOntoParent(ItemEvent.SlotClick<PaperNode, PaperItem> event, PaperNode parent, PaperNode child) {
            return event instanceof PaperItemEvent.SlotClick paper
                    ? new Events.CombineNOntoParent(parent, paper.user(), paper.slot(), paper.item(), this, child)
                    : null;
        }

        @Override
        protected void openNodeView(ItemEvent.SlotClick<PaperNode, PaperItem> event, int amount) {
            if (!(event instanceof PaperItemEvent.SlotClick paper)) return;
            if (!(event.user() instanceof PlayerUser user)) return;
            ChestInterface.builder()
                    .rows(6)
                    .addTransform(new PaperNodeView<>(platform, event.tree(), options, amount, paper.handle().getSlot(), node ->
                            event.slot().set(node.createItem(user))))
                    .build()
                    .open(PlayerViewer.of(user.entity()), platform.lc().safe(user.locale(), "node_view.title",
                            "node", event.item().name()));
        }

        @Override
        public void save(Type type, ConfigurationNode node) throws SerializationException {}

        @Override
        public void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) throws IllegalArgumentException {}

        @Override
        public Instance copy(PaperNode parent) {
            return new Instance(parent);
        }
    }
    
    public static final class Events {
        private Events() {}
        
        public static final class CombineNOntoParent extends PaperItemEvent.BaseCancellable
                implements NodeViewFeature.Events.CombineNOntoParent<PaperNode, PaperItem, Instance> {
            private final PaperNodeViewFeature.Instance feature;
            private final PaperNode parent;

            public CombineNOntoParent(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, Instance feature, PaperNode parent) {
                super(node, user, slot, item);
                this.feature = feature;
                this.parent = parent;
            }

            @Override public Instance feature() { return feature; }
            @Override public PaperNode parent() { return parent; }
        }

        public static final class CombineChildOntoN extends PaperItemEvent.BaseCancellable
                implements NodeViewFeature.Events.CombineChildOntoN<PaperNode, PaperItem, Instance> {
            private final PaperNodeViewFeature.Instance feature;
            private final PaperNode child;

            public CombineChildOntoN(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, Instance feature, PaperNode child) {
                super(node, user, slot, item);
                this.feature = feature;
                this.child = child;
            }

            @Override public Instance feature() { return feature; }
            @Override public PaperNode child() { return child; }
        }
    }
}
