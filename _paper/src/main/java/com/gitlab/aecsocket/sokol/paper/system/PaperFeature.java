package com.gitlab.aecsocket.sokol.paper.system;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.display.Particles;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.sokol.core.feature.LoadProvider;
import com.gitlab.aecsocket.sokol.core.feature.Feature;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.feature.util.Availability;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.Animation;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.util.List;
import java.util.Objects;

public interface PaperFeature extends Feature {
    final class Serializer implements TypeSerializer<PaperFeature.Instance> {
        private final SokolPlugin plugin;
        private PaperTreeNode base;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        public SokolPlugin plugin() { return plugin; }

        public PaperTreeNode base() { return base; }
        public void base(PaperTreeNode component) { this.base = component; }

        @Override
        public void serialize(java.lang.reflect.Type type, PaperFeature.@Nullable Instance obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                obj.save(type, node);
            }
        }

        @Override
        public Instance deserialize(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
            if (base == null)
                throw new SerializationException("No base component provided");

            String id = Objects.toString(node.key());
            PaperFeature system = base.value().baseSystem(id)
                    .orElseThrow(() -> new SerializationException("No system with ID [" + id + "] on component [" + base.value().id() + "]"));
            return system.load(base, type, node);
        }
    }

    interface Instance extends Feature.Instance {
        @Override
        PaperFeature base();
        @Override SokolPlugin platform();

        @Override
        default void runAction(Availability avail, String key, ItemUser user, ItemSlot slot, @Nullable Vector3 position) {
            Feature.Instance.super.runAction(avail, key, user, slot, position);
            if (!(user instanceof PaperUser paper))
                return;
            Location location = position == null ? paper.location() : PaperUtils.toBukkit(position, paper.location().getWorld());
            parent().stats().<List<PreciseSound>>val(key + "_sounds")
                    .ifPresent(v -> v.forEach(s -> s.play(platform(), location)));
            parent().stats().<List<Particles>>val(key + "_particles")
                    .ifPresent(v -> v.forEach(p -> p.spawn(location)));
            if (user instanceof PlayerUser player) {
                parent().stats().<Animation>val(key + "_animation").ifPresent(anim -> anim.start(platform(), player.handle(), slot));
            }
        }

        default @Nullable PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException { return null; }
        default void save(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {}
    }

    interface ConfigType {
        PaperFeature createSystem(ConfigurationNode cfg) throws SerializationException;
    }

    interface LoadProviderType {
        LoadProvider createLoadProvider();
    }

    interface KeyedType extends ConfigType, LoadProviderType, Keyed {}

    Instance load(PaperTreeNode node, PersistentDataContainer data) throws IllegalArgumentException;
    Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException;
}
