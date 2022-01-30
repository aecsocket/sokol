package com.github.aecsocket.sokol.paper.world;

import java.util.Collections;
import java.util.Locale;

import com.github.aecsocket.minecommons.core.effect.Effector;
import com.github.aecsocket.minecommons.core.effect.ForwardingEffector;
import com.github.aecsocket.minecommons.core.effect.ParticleEffect;
import com.github.aecsocket.minecommons.core.effect.SoundEffect;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PlayerEffector;
import com.github.aecsocket.sokol.paper.SokolPlugin;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;

/* package */ final class PaperItemUserImpl {
    private PaperItemUserImpl() {}

    static class OfEntity<E extends Entity> implements PaperItemUser.OfEntity, ForwardingAudience {
        protected final SokolPlugin plugin;
        protected final E entity;
        protected final Iterable<E> audiences;

        public OfEntity(SokolPlugin plugin, E entity) {
            this.plugin = plugin;
            this.entity = entity;
            audiences = Collections.singleton(entity);
        }

        @Override public E entity() { return entity; }

        @Override public Locale locale() { return plugin.defaultLocale(); }
        @Override public Location location() { return entity.getLocation(); }
        @Override public Vector3 position() { return PaperUtils.toCommons(location()); }
        @Override public Vector3 direction() { return PaperUtils.toCommons(location().getDirection()); }

        @Override public Iterable<? extends Audience> audiences() { return audiences; }

        @Override public void play(SoundEffect effect, Vector3 origin) {}
        @Override public void spawn(ParticleEffect effect, Vector3 origin) {}
    }

    static class OfLivingEntity<E extends LivingEntity> extends OfEntity<E> implements PaperItemUser.OfLivingEntity {
        public OfLivingEntity(SokolPlugin plugin, E entity) {
            super(plugin, entity);
        }

        @Override public Location location() { return entity.getEyeLocation(); }
    }

    static class OfPlayer<E extends Player> extends OfLivingEntity<E> implements PaperItemUser.OfPlayer, ForwardingEffector {
        protected final Iterable<PlayerEffector> effectors;

        public OfPlayer(SokolPlugin plugin, E entity) {
            super(plugin, entity);
            effectors = Collections.singleton(new PlayerEffector(plugin.effectors(), entity));
        }

        @Override public Locale locale() { return entity.locale(); }
        @Override public Iterable<? extends Effector> effectors() { return effectors; }
    }
}
