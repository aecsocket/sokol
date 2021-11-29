package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.sokol.core.event.ItemEvent;
import com.gitlab.aecsocket.sokol.core.feature.NodeViewFeature;
import com.gitlab.aecsocket.sokol.core.nodeview.NodeView;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.event.PaperItemEvent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNodeView;
import com.gitlab.aecsocket.sokol.paper.stat.SoundsStat;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperItemSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import io.leangen.geantyref.TypeToken;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.incendo.interfaces.core.view.InterfaceView;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.type.ChestInterface;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;

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
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types().build();

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
    public Instance create(PaperNode node) {
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

        @Override protected TypeToken<PaperItemEvent.SlotClick> eventSlotClick() { return new TypeToken<>() {}; }
        @Override protected TypeToken<PaperItemEvent.SlotDrag> eventSlotDrag() { return new TypeToken<>() {}; }
        @Override protected TypeToken<Events.CombineOntoParent> eventCombineOntoParent() { return new TypeToken<>() {}; }
        @Override protected TypeToken<PaperNodeView.Events.InsertInto> eventInsertInto() { return new TypeToken<>() {}; }
        @Override protected TypeToken<PaperNodeView.Events.RemoveFrom> eventRemoveFrom() { return new TypeToken<>() {}; }

        @Override
        protected boolean cancelIfClickedViewedItem(ItemEvent.SlotClick<PaperNode, PaperItem> event) {
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
        protected boolean cancelIfClickedViewedItem(ItemEvent.SlotDrag<PaperNode, PaperItem> event) {
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
        protected boolean callCombineOntoParent(ItemEvent.SlotClick<PaperNode, PaperItem> event, PaperNode node, PaperNode parent) {
            if (!(event instanceof PaperItemEvent.SlotClick paper)) return false;
            return treeCtx.call(new Events.CombineOntoParent(node, paper.user(), paper.slot(), paper.item(), this, parent)).cancelled();
        }

        @Override
        protected boolean callCombineChildOnto(ItemEvent.SlotClick<PaperNode, PaperItem> event, PaperNode node, PaperNode child) {
            if (!(event instanceof PaperItemEvent.SlotClick paper)) return false;
            return treeCtx.call(new Events.CombineChildOnto(node, paper.user(), paper.slot(), paper.item(), this, parent)).cancelled();
        }

        @Override
        protected void openNodeView(ItemEvent.SlotClick<PaperNode, PaperItem> event, int amount) {
            if (!(event instanceof PaperItemEvent.SlotClick paper)) return;
            if (!(event.user() instanceof PlayerUser user)) return;
            ChestInterface.builder()
                    .rows(6)
                    .addTransform(new PaperNodeView<>(platform, event.node(), options, amount, paper.handle().getSlot(), node ->
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
        
        public static final class CombineOntoParent extends PaperItemEvent.BaseCancellable
                implements NodeViewFeature.Events.CombineOntoParent<PaperNode, PaperItem, PaperNodeViewFeature.Instance> {
            private final PaperNodeViewFeature.Instance feature;
            private final PaperNode parent;

            public CombineOntoParent(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, Instance feature, PaperNode parent) {
                super(node, user, slot, item);
                this.feature = feature;
                this.parent = parent;
            }

            @Override public Instance feature() { return feature; }
            @Override public PaperNode parent() { return parent; }
        }

        public static final class CombineChildOnto extends PaperItemEvent.BaseCancellable
                implements NodeViewFeature.Events.CombineChildOnto<PaperNode, PaperItem, PaperNodeViewFeature.Instance> {
            private final PaperNodeViewFeature.Instance feature;
            private final PaperNode child;

            public CombineChildOnto(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, Instance feature, PaperNode child) {
                super(node, user, slot, item);
                this.feature = feature;
                this.child = child;
            }

            @Override public Instance feature() { return feature; }
            @Override public PaperNode child() { return child; }
        }
    }
}
