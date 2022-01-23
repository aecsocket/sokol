package old.feature;

import com.github.aecsocket.sokol.core.event.ItemEvent;
import com.github.aecsocket.sokol.core.feature.NodeHolderFeature;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.minecommons.core.Quantifier;

import old.FeatureType;
import old.SokolPlugin;
import old.event.PaperItemEvent;
import old.impl.PaperFeature;
import old.impl.PaperFeatureInstance;
import old.impl.PaperNode;
import old.wrapper.PaperItem;
import old.wrapper.slot.PaperItemSlot;
import old.wrapper.user.PaperUser;
import io.leangen.geantyref.TypeToken;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.*;

public final class PaperNodeHolderFeature extends NodeHolderFeature<PaperNodeHolderFeature.Instance, PaperNode, PaperItem>
        implements PaperFeature<PaperNodeHolderFeature.Instance> {
    public static final StatTypes STAT_TYPES = StatTypes.types();
    public static final Map<String, Class<? extends NodeRule>> RULE_TYPES = NodeRule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new PaperNodeHolderFeature(platform,
            config.node("listener_priority").getInt(PRIORITY_DEFAULT),
            NodeRule.Constant.TRUE,
            config.node("header_position").get(Position.class, Position.TOP),
            config.node("capacity").get(Integer.class),
            config.node("show_full_as_durability").getBoolean(true)
    ));

    private final SokolPlugin platform;

    public PaperNodeHolderFeature(SokolPlugin platform, int listenerPriority, NodeRule rule, Position headerPosition, @Nullable Integer capacity, boolean showFullAsDurability) {
        super(listenerPriority, rule, headerPosition, capacity, showFullAsDurability);
        this.platform = platform;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {
        rule = config.node("rule").get(NodeRule.class);
    }

    @Override public SokolPlugin platform() { return platform; }

    @Override
    public Instance load(PaperNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperNode node, Type type, ConfigurationNode config) throws SerializationException {
        return new Instance(node, new LinkedList<>(config.node("nodes").getList(new TypeToken<Quantifier<PaperNode>>(){}, Collections.emptyList())));
    }

    @Override
    public Instance load(PaperNode node, PersistentDataContainer pdc) throws IllegalArgumentException {
        PersistentDataContainer[] fNodes = pdc.get(platform.key("nodes"), PersistentDataType.TAG_CONTAINER_ARRAY);
        if (fNodes == null)
            return new Instance(node);
        LinkedList<Quantifier<PaperNode>> nodes = new LinkedList<>();
        for (var fQt : fNodes) {
            //noinspection ConstantConditions
            platform.persistence().safeLoad(fQt.get(platform.key("value"), PersistentDataType.TAG_CONTAINER))
                            .ifPresent(value -> nodes.add(new Quantifier<>(
                                    value,
                                    fQt.getOrDefault(platform.key("amount"), PersistentDataType.INTEGER, 0)
                            )));
        }
        return new Instance(node, nodes);
    }

    public final class Instance extends NodeHolderFeature<Instance, PaperNode, PaperItem>.Instance implements PaperFeatureInstance {
        public Instance(PaperNode parent, LinkedList<Quantifier<PaperNode>> nodes) {
            super(parent, nodes);
        }

        public Instance(PaperNode parent, Instance o) {
            super(parent, o);
        }

        public Instance(PaperNode parent) {
            super(parent);
        }

        @Override
        protected boolean equal(PaperNode a, PaperNode b) {
            // TODO THIS IS INCREDIBLY SCUFFED
            // FIX THIS JESUS CHRIST
            ConfigurationNode config = platform.loaderBuilder().build().createNode();
            try {
                return config.copy().set(a)
                        .equals(config.copy().set(b));
            } catch (SerializationException e) {
                return false;
            }
        }

        @Override
        protected Events.@Nullable NodeAdd createNodeAdd(ItemEvent.SlotClick<PaperNode, PaperItem> event, PaperNode held, PaperItem heldItem) {
            return event instanceof PaperItemEvent.SlotClick paper
                    ? new Events.NodeAdd(event.tree(), paper.user(), paper.slot(), paper.item(), this, held, paper.cursor(), heldItem)
                    : null;
        }

        @Override
        protected Events.@Nullable NodeRemove createNodeRemove(ItemEvent.SlotClick<PaperNode, PaperItem> event, PaperNode held) {
            return event instanceof PaperItemEvent.SlotClick paper
                    ? new Events.NodeRemove(event.tree(), paper.user(), paper.slot(), paper.item(), this, held, paper.cursor())
                    : null;
        }

        @Override
        public void save(Type type, ConfigurationNode node) throws SerializationException {
            node.node("nodes").set(nodes);
        }

        @Override
        public void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer[] fNodes = new PersistentDataContainer[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                var qt = nodes.get(i);
                PersistentDataContainer fQt = ctx.newPersistentDataContainer();
                fQt.set(platform.key("amount"), PersistentDataType.INTEGER, qt.amount());
                fQt.set(platform.key("value"), PersistentDataType.TAG_CONTAINER, platform.persistence().save(ctx.newPersistentDataContainer(), qt.object()));
                fNodes[i] = fQt;
            }
            pdc.set(platform.key("nodes"), PersistentDataType.TAG_CONTAINER_ARRAY, fNodes);
        }

        @Override
        public Instance copy(PaperNode parent) {
            return new Instance(parent, this);
        }
    }
    
    public static final class Events {
        private Events() {}

        public static class NodeModify extends PaperItemEvent.BaseCancellable
                implements NodeHolderFeature.Events.NodeModify<PaperNode, PaperItem, PaperNodeHolderFeature.Instance> {
            private final Instance feature;
            private final PaperNode held;
            private final PaperItemSlot cursor;

            public NodeModify(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, Instance feature, PaperNode held, PaperItemSlot cursor) {
                super(node, user, slot, item);
                this.feature = feature;
                this.held = held;
                this.cursor = cursor;
            }

            @Override public Instance feature() { return feature; }
            @Override public PaperNode held() { return held; }
            @Override public PaperItemSlot cursor() { return cursor; }
        }

        public static final class NodeAdd extends NodeModify
            implements NodeHolderFeature.Events.NodeAdd<PaperNode, PaperItem, PaperNodeHolderFeature.Instance> {
            private final PaperItem heldItem;

            public NodeAdd(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, Instance feature, PaperNode held, PaperItemSlot cursor, PaperItem heldItem) {
                super(node, user, slot, item, feature, held, cursor);
                this.heldItem = heldItem;
            }

            @Override public PaperItem heldItem() { return heldItem; }
        }

        public static final class NodeRemove extends NodeModify
                implements NodeHolderFeature.Events.NodeRemove<PaperNode, PaperItem, PaperNodeHolderFeature.Instance> {
            public NodeRemove(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, Instance feature, PaperNode held, PaperItemSlot cursor) {
                super(node, user, slot, item, feature, held, cursor);
            }
        }
    }
}
