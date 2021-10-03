package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.comphenix.protocol.PacketType;
import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
import com.gitlab.aecsocket.sokol.core.feature.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.feature.LoadProvider;
import com.gitlab.aecsocket.sokol.core.feature.inbuilt.ItemFeature;
import com.gitlab.aecsocket.sokol.core.feature.inbuilt.SchedulerFeature;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import com.gitlab.aecsocket.sokol.core.util.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.util.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.core.wrapper.UserSlot;
import com.gitlab.aecsocket.sokol.paper.*;
import com.gitlab.aecsocket.sokol.paper.stat.AnimationStat;
import com.gitlab.aecsocket.sokol.paper.stat.EffectsStat;
import com.gitlab.aecsocket.sokol.paper.stat.SoundsStat;
import com.gitlab.aecsocket.sokol.paper.system.PaperFeature;
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

public class PropertiesFeature extends AbstractFeature implements PaperFeature {
    public static final String ID = "properties";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final EffectsStat STAT_HOLD_EFFECTS = EffectsStat.effectsStat("hold_effects");
    public static final SDouble STAT_WALK_SPEED = doubleStat("walk_speed");
    public static final SDouble STAT_ATTACK_DAMAGE = doubleStat("attack_damage");
    public static final SBoolean STAT_BLOCK_INTERACTION = booleanStat("block_interaction");
    public static final SBoolean STAT_UNBREAKABLE = booleanStat("unbreakable");

    public static final SLong STAT_EQUIP_DELAY = longStat("equip_delay");
    public static final SLong STAT_EQUIP_SOUNDS = longStat("equip_sounds");
    public static final AnimationStat STAT_EQUIP_ANIMATION = animationStat("equip_animation");

    public static final SBoolean STAT_ALLOW_SPRINT = booleanStat("allow_sprint");
    public static final SLong STAT_SPRINT_START_DELAY = longStat("sprint_start_delay");
    public static final SoundsStat STAT_SPRINT_START_SOUNDS = soundsStat("sprint_start_sounds");
    public static final AnimationStat STAT_SPRINT_START_ANIMATION = animationStat("sprint_start_animation");

    public static final SLong STAT_SPRINT_STOP_DELAY = longStat("sprint_stop_delay");
    public static final SoundsStat STAT_SPRINT_STOP_SOUNDS = soundsStat("sprint_stop_sounds");
    public static final AnimationStat STAT_SPRINT_STOP_ANIMATION = animationStat("sprint_stop_animation");

    public static final StatTypes STATS = StatTypes.of(
            STAT_HOLD_EFFECTS, STAT_WALK_SPEED, STAT_ATTACK_DAMAGE, STAT_BLOCK_INTERACTION, STAT_UNBREAKABLE,
            STAT_EQUIP_DELAY, STAT_EQUIP_SOUNDS, STAT_EQUIP_ANIMATION,
            STAT_ALLOW_SPRINT, STAT_SPRINT_START_DELAY, STAT_SPRINT_START_SOUNDS, STAT_SPRINT_START_ANIMATION,
            STAT_SPRINT_STOP_DELAY, STAT_SPRINT_STOP_SOUNDS, STAT_SPRINT_STOP_ANIMATION
    );
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofStats(ID, STATS);
    public static final UUID MOVE_SPEED_ATTRIBUTE = UUID.randomUUID();
    public static final UUID ATTACK_DAMAGE_ATTRIBUTE = UUID.randomUUID();

    public final class Instance extends AbstractFeature.Instance implements PaperFeature.Instance {
        private SchedulerFeature<?>.Instance scheduler;

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PropertiesFeature base() { return PropertiesFeature.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        public void build(StatLists stats) {
            scheduler = depend(SchedulerFeature.KEY);
            parent.events().register(TreeEvent.Update.class, this::event);
            parent.events().register(ItemFeature.Events.CreateItem.class, this::event);
            parent.events().register(ItemTreeEvent.Hold.class, this::event);
            parent.events().register(ItemTreeEvent.Input.class, this::event);
            parent.events().register(ItemTreeEvent.Equip.class, this::event);
            parent.events().register(ItemTreeEvent.Unequip.class, this::event);
            parent.events().register(ItemTreeEvent.Break.class, this::event);
            parent.events().register(ItemTreeEvent.BlockBreak.class, this::event);
            parent.events().register(ItemTreeEvent.BlockPlace.class, this::event);
        }

        public void update(ItemUser user) {
            if (user instanceof PlayerUser player) {
                Player handle = player.handle();
                platform.protocol().send(handle, PacketType.Play.Server.UPDATE_HEALTH, packet -> {
                    packet.getFloat().write(0, (float) handle.getHealth());
                    packet.getFloat().write(1, handle.getSaturation());
                    packet.getIntegers().write(0, parent.stats().val(STAT_ALLOW_SPRINT).orElse(true) ? handle.getFoodLevel() : 6);
                });
            }
        }

        private void event(TreeEvent.Update event) {
            if (!parent.isRoot())
                return;
            update(event.user());
        }

        private void event(ItemFeature.Events.CreateItem event) {
            if (!parent.isRoot())
                return;
            if (event.item() instanceof PaperItemStack item) {
                PaperUtils.modify(item.handle(), meta -> {
                    parent.stats().val(STAT_WALK_SPEED).ifPresent(walkSpeed ->
                            meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(MOVE_SPEED_ATTRIBUTE,
                                    "moveSpeed", walkSpeed - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1)));
                    parent.stats().val(STAT_ATTACK_DAMAGE).ifPresent(attackDamage ->
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
                parent.stats().val(STAT_EQUIP_DELAY).ifPresent(delay -> {
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

        protected void event(ItemTreeEvent.Break event) {
            if (!parent.isRoot())
                return;
            parent.stats().val(STAT_UNBREAKABLE)
                    .ifPresent(v -> { if (v) event.cancel(); });
        }

        protected void event(ItemTreeEvent.Hold event) {
            if (!parent.isRoot())
                return;
            if (!event.sync())
                return;

            if (event.user() instanceof LivingEntityUser living) {
                parent.stats().val(STAT_HOLD_EFFECTS)
                        .ifPresent(living.handle()::addPotionEffects);
            }
        }

        protected void event(ItemTreeEvent.BlockBreak event) {
            if (!parent.isRoot())
                return;
            parent.stats().val(STAT_BLOCK_INTERACTION).ifPresent(allow -> {
                if (!allow)
                    event.cancel();
            });
        }

        protected void event(ItemTreeEvent.BlockPlace event) {
            if (!parent.isRoot())
                return;
            parent.stats().val(STAT_BLOCK_INTERACTION).ifPresent(allow -> {
                if (!allow)
                    event.cancel();
            });
        }
    }

    private final SokolPlugin platform;

    public PropertiesFeature(SokolPlugin platform, int listenerPriority) {
        super(listenerPriority);
        this.platform = platform;
    }

    @Override public String id() { return ID; }

    public SokolPlugin platform() { return platform; }

    @Override public StatTypes statTypes() { return STATS; }

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
        return cfg -> new PropertiesFeature(platform,
                cfg.node(keyListenerPriority).getInt());
    }
}
