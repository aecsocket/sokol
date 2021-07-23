package com.gitlab.aecsocket.sokol.paper.wrapper.item;

import com.comphenix.protocol.PacketType;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.plugin.ProtocolConstants;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.EquipSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.InventorySlot;
import io.leangen.geantyref.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Animation extends ArrayList<Animation.Frame> {
    public static final class Serializer implements TypeSerializer<Animation> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Type type, @Nullable Animation obj, ConfigurationNode node) throws SerializationException {
            node.set(new TypeToken<List<Frame>>() {}, obj);
        }

        @Override
        public Animation deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new Animation(Serializers.require(node, new TypeToken<List<Frame>>() {}));
        }
    }

    public Animation(int initialCapacity) { super(initialCapacity); }
    public Animation() {}
    public Animation(@NotNull Collection<? extends Frame> c) { super(c); }

    public void start(SokolPlugin plugin, Player player, int defSlot) {
        plugin.schedulers().playerData(player).startAnimation(new Instance(plugin, player, defSlot));
    }

    public void start(SokolPlugin plugin, Player player, EquipmentSlot defSlot) {
        start(plugin, player, ProtocolConstants.SLOT_IDS.getOrDefault(defSlot, player.getInventory().getHeldItemSlot()));
    }

    public void start(SokolPlugin plugin, Player player, InventorySlot defSlot) {
        start(plugin, player, defSlot.slot());
    }

    public void start(SokolPlugin plugin, Player player, EquipSlot defSlot) {
        start(plugin, player, defSlot.slot());
    }

    public void start(SokolPlugin plugin, Player player, ItemSlot defSlot) {
        if (defSlot instanceof EquipSlot equip)
            start(plugin, player, equip.slot());
        if (defSlot instanceof InventorySlot inv)
            start(plugin, player, inv.slot());
    }

    public record NumberProperty(int value, boolean additive) {
        public static final class Serializer implements TypeSerializer<NumberProperty> {
            public static final Serializer INSTANCE = new Serializer();

            @Override
            public void serialize(Type type, @Nullable NumberProperty obj, ConfigurationNode node) throws SerializationException {
                if (obj == null) node.set(null);
                else {
                    if (obj.additive) {
                        node.appendListNode().set(obj.value);
                    } else
                        node.set(obj.value);
                }
            }

            @Override
            public NumberProperty deserialize(Type type, ConfigurationNode node) throws SerializationException {
                if (node.isList()) {
                    if (node.childrenList().size() != 1)
                        throw new SerializationException(node, type, "If list, must be of size 1");
                    return new NumberProperty(Serializers.require(node.node(0), int.class), true);
                }
                return new NumberProperty(Serializers.require(node, int.class), false);
            }
        }
    }

    @ConfigSerializable
    public record Frame(
            @Required long duration,
            @Nullable EquipmentSlot slot,
            @Nullable ItemDescriptor item,
            @Nullable NumberProperty modelData,
            @Nullable NumberProperty damage
    ) {
        public static final class Serializer implements TypeSerializer<Frame> {
            public static final Serializer INSTANCE = new Serializer();

            @Override
            public void serialize(Type type, @Nullable Frame obj, ConfigurationNode node) throws SerializationException {
                if (obj == null) node.set(null);
                else {
                    node.node("duration").set(obj.duration);
                    node.node("slot").set(obj.slot);
                    node.node("item").set(obj.item);
                    node.node("model_data").set(obj.modelData);
                    node.node("damage").set(obj.damage);
                }
            }

            @Override
            public Frame deserialize(Type type, ConfigurationNode node) throws SerializationException {
                if (node.hasChild("range")) {
                    return new Frame(0, null, null, null, null); // todo
                } else {
                    return new Frame(
                            Serializers.require(node.node("duration"), long.class),
                            node.node("slot").get(EquipmentSlot.class),
                            node.node("item").get(ItemDescriptor.class),
                            node.node("model_data").get(NumberProperty.class),
                            node.node("damage").get(NumberProperty.class)
                    );
                }
            }
        }

        public void apply(SokolPlugin plugin, Player player, int defSlot) {
            int slotId = slot == null ? defSlot : ProtocolConstants.SLOT_IDS.get(slot);
            ItemStack item = player.getInventory().getItem(slotId);
            if (item == null)
                return;
            plugin.protocol().send(player, PacketType.Play.Server.SET_SLOT, packet -> {
                packet.getIntegers().write(0, -2);
                packet.getIntegers().write(1, slotId);
                ItemStack sent = this.item == null
                        ? PaperUtils.modify(item.clone(), meta -> {
                            if (modelData != null)
                                meta.setCustomModelData(modelData.additive ? meta.getCustomModelData() + modelData.value : modelData.value);
                            if (damage != null && meta instanceof Damageable damageable)
                                damageable.setDamage(damage.additive ? damageable.getDamage() + damage.value : damage.value);
                        }) : this.item.apply(item.clone());
                plugin.packetListener().showUpdate(sent);
                packet.getItemModifier().write(0, sent);
            });
        }
    }

    public final class Instance {
        private final SokolPlugin plugin;
        private final Player player;
        private final int defSlot;
        private int index;
        private Frame frame;
        private long frameTime;
        private int iterations;

        public Instance(SokolPlugin plugin, Player player, int defSlot) {
            this.plugin = plugin;
            this.player = player;
            this.defSlot = defSlot;
        }

        public Animation animation() { return Animation.this; }

        public SokolPlugin plugin() { return plugin; }
        public Player player() { return player; }
        public int defSlot() { return defSlot; }

        public int index() { return index; }
        public void index(int index) { this.index = index; }

        public Frame frame() { return frame; }
        public void frame(Frame frame) { this.frame = frame; }

        public long frameTime() { return frameTime; }
        public void frameTime(long frameTime) { this.frameTime = frameTime; }

        public boolean finished() { return index >= size(); }

        public void apply() {
            frame.apply(plugin, player, defSlot);
        }

        public void updateFrame() {
            frame = get(index);
        }

        public void nextFrame() {
            ++index;
            if (finished())
                return;
            updateFrame();
            apply();
        }

        public void tick(TaskContext ctx) {
            // fix timing issues to do with item updates being sent
            ++iterations;
            if (iterations <= 2)
                return;
            frameTime += ctx.delta();
            if (frame == null) {
                updateFrame();
                apply();
            }
            while (!finished() && frameTime >= frame.duration) {
                frameTime -= frame.duration;
                nextFrame();
            }
        }
    }
}
