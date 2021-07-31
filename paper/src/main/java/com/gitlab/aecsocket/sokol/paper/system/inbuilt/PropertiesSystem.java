package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.comphenix.protocol.PacketType;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.ItemSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.core.wrapper.UserSlot;
import com.gitlab.aecsocket.sokol.paper.*;
import com.gitlab.aecsocket.sokol.paper.stat.EffectsStat;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.PaperItemStack;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingEntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.AnimationStat.*;

public class PropertiesSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "properties";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("hold_effects", EffectsStat.effectsStat())
            .put("walk_speed", doubleStat())
            .put("attack_damage", doubleStat())
            .put("block_interaction", booleanStat())

            .put("equip_delay", longStat())
            .put("equip_sounds", soundsStat())
            .put("equip_animation", animationStat())

            .put("allow_sprint", booleanStat())
            .put("sprint_start_delay", longStat())
            .put("sprint_start_sounds", soundsStat())
            .put("sprint_start_animation", animationStat())
            .put("sprint_stop_delay", longStat())
            .put("sprint_stop_sounds", soundsStat())
            .put("sprint_stop_animation", animationStat())
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofStats(ID, STATS);
    public static final UUID MOVE_SPEED_ATTRIBUTE = UUID.randomUUID();
    public static final UUID ATTACK_DAMAGE_ATTRIBUTE = UUID.randomUUID();

    public final class Instance extends AbstractSystem.Instance implements PaperSystem.Instance {
        private SchedulerSystem<?>.Instance scheduler;

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PropertiesSystem base() { return PropertiesSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        public void build(StatLists stats) {
            scheduler = depend(SchedulerSystem.KEY);
            parent.events().register(TreeEvent.Update.class, this::event);
            parent.events().register(ItemSystem.Events.CreateItem.class, this::event);
            parent.events().register(ItemTreeEvent.Input.class, this::event);
            parent.events().register(ItemTreeEvent.Equip.class, this::event);
            parent.events().register(ItemTreeEvent.Unequip.class, this::event);
            parent.events().register(ItemTreeEvent.Hold.class, this::event);
            parent.events().register(ItemTreeEvent.BlockBreak.class, this::event);
            parent.events().register(ItemTreeEvent.BlockPlace.class, this::event);
        }

        public void update(ItemUser user) {
            if (user instanceof PlayerUser player) {
                Player handle = player.handle();
                platform.protocol().send(handle, PacketType.Play.Server.UPDATE_HEALTH, packet -> {
                    packet.getFloat().write(0, (float) handle.getHealth());
                    packet.getFloat().write(1, handle.getSaturation());
                    packet.getIntegers().write(0, parent.stats().<Boolean>val("allow_sprint").orElse(true) ? handle.getFoodLevel() : 6);
                });
            }
        }

        private void event(TreeEvent.Update event) {
            if (!parent.isRoot())
                return;
            update(event.user());
        }

        private void event(ItemSystem.Events.CreateItem event) {
            if (!parent.isRoot())
                return;
            if (event.item() instanceof PaperItemStack item) {
                PaperUtils.modify(item.handle(), meta -> {
                    parent.stats().<Double>val("walk_speed").ifPresent(walkSpeed ->
                            meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(MOVE_SPEED_ATTRIBUTE,
                                    "moveSpeed", walkSpeed - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1)));
                    parent.stats().<Double>val("attack_damage").ifPresent(attackDamage ->
                            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_ATTRIBUTE,
                                    "attackDamage", attackDamage, AttributeModifier.Operation.ADD_NUMBER)));
                });
            }
        }

        private void event(ItemTreeEvent.Input event) {
            if (!parent.isRoot())
                return;
            if (event.input() == InputType.SPRINT_START)
                runAction(scheduler, "sprint_start", event.user(), event.slot(), null);
            if (event.input() == InputType.SPRINT_STOP)
                runAction(scheduler, "sprint_stop", event.user(), event.slot(), null);
            update(event.user());
        }

        private void event(ItemTreeEvent.Equip event) {
            if (!parent.isRoot())
                return;
            update(event.user());
            runAction(scheduler, "equip", event.user(), event.slot(), null);
            if (event.user() instanceof PlayerUser player && event.slot() instanceof UserSlot slot && slot.inHand()) {
                parent.stats().<Long>val("equip_delay").ifPresent(delay -> {
                    player.handle().setCooldown(slot.get()
                            .map(s -> s instanceof PaperItemStack paper ? paper.handle().getType() : null)
                            .orElseThrow(IllegalStateException::new), (int) Ticks.ticks(delay));
                    player.handle().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                            (int) Ticks.ticks(delay), 127, false, false, false));
                });
            }
            event.update();
        }

        protected void event(ItemTreeEvent.Unequip event) {
            if (!parent.isRoot())
                return;
            update(event.user());
        }

        protected void event(ItemTreeEvent.Hold event) {
            if (!parent.isRoot())
                return;
            if (!event.sync())
                return;

            if (event.user() instanceof LivingEntityUser living) {
                parent.stats().<List<PotionEffect>>val("hold_effects")
                        .ifPresent(living.handle()::addPotionEffects);
            }
        }

        protected void event(ItemTreeEvent.BlockBreak event) {
            if (!parent.isRoot())
                return;
            parent.stats().<Boolean>val("block_interaction").ifPresent(allow -> {
                if (!allow)
                    event.cancel();
            });
        }

        protected void event(ItemTreeEvent.BlockPlace event) {
            if (!parent.isRoot())
                return;
            parent.stats().<Boolean>val("block_interaction").ifPresent(allow -> {
                if (!allow)
                    event.cancel();
            });
        }
    }

    private final SokolPlugin platform;

    public PropertiesSystem(SokolPlugin platform, int listenerPriority) {
        super(listenerPriority);
        this.platform = platform;
    }

    @Override public String id() { return ID; }

    public SokolPlugin platform() { return platform; }

    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node);
    }

    public static ConfigType type(SokolPlugin platform) {
        return cfg -> new PropertiesSystem(platform,
                cfg.node(keyListenerPriority).getInt());
    }
}
