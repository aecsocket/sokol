package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.comphenix.protocol.PacketType;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.*;
import com.gitlab.aecsocket.sokol.paper.stat.EffectsStat;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingEntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
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

            .put("equip_delay", longStat())
            .put("equip_sound", soundsStat())
            .put("equip_animation", animationStat())

            .put("allow_sprint", booleanStat())
            .put("sprint_start_delay", longStat())
            .put("sprint_start_sound", soundsStat())
            .put("sprint_start_animation", animationStat())
            .put("sprint_stop_delay", longStat())
            .put("sprint_stop_sound", soundsStat())
            .put("sprint_stop_animation", animationStat())
            .build();

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
            parent.events().register(ItemTreeEvent.Input.class, this::event);
            parent.events().register(ItemTreeEvent.Equip.class, this::event);
            parent.events().register(ItemTreeEvent.Unequip.class, this::event);
            parent.events().register(ItemTreeEvent.Hold.class, this::event);
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

        private void event(ItemTreeEvent.Input event) {
            if (!parent.isRoot())
                return;
            if (event.input() == InputType.SPRINT_START)
                runAction(scheduler, event.user(), event.slot(), "sprint_start");
            if (event.input() == InputType.SPRINT_STOP)
                runAction(scheduler, event.user(), event.slot(), "sprint_stop");
            update(event.user());
        }

        private void event(ItemTreeEvent.Equip event) {
            if (!parent.isRoot())
                return;
            update(event.user());
            runAction(scheduler, event.user(), event.slot(), "equip");
        }

        private void event(ItemTreeEvent.Unequip event) {
            if (!parent.isRoot())
                return;
            update(event.user());
        }

        private void event(ItemTreeEvent.Hold event) {
            if (!parent.isRoot())
                return;
            if (!event.sync())
                return;

            if (event.user() instanceof LivingEntityUser living) {
                parent.stats().<List<PotionEffect>>desc("hold_effects")
                        .ifPresent(living.handle()::addPotionEffects);
            }
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

    public static Type type(SokolPlugin platform) {
        return cfg -> new PropertiesSystem(platform,
                cfg.node(keyListenerPriority).getInt());
    }
}
