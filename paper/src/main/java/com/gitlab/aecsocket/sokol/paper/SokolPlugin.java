package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Files;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.core.scheduler.Task;
import com.gitlab.aecsocket.minecommons.core.scheduler.ThreadScheduler;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.feature.StatDisplayFeature;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.Registry;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.paper.feature.DummyFeature;
import com.gitlab.aecsocket.sokol.paper.feature.PaperStatDisplayFeature;
import com.gitlab.aecsocket.sokol.paper.impl.*;
import io.leangen.geantyref.TypeToken;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform {
    public static final String FILE_EXTENSION = "conf";
    public static final String PATH_COMPONENT = "component";
    public static final String PATH_BLUEPRINT = "blueprint";
    public static final String PERMISSION_PREFIX = "sokol";
    public static final int BSTATS_ID = 11870;

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<PaperBlueprint> blueprints = new Registry<>();
    private final Registry<FeatureType.Keyed> featureTypes = new Registry<>();
    private final SokolPersistence persistence = new SokolPersistence(this);
    private final PaperScheduler paperScheduler = new PaperScheduler(this);
    private final ThreadScheduler threadScheduler = new ThreadScheduler(Executors.newSingleThreadExecutor());
    private final Map<Player, PlayerData> playerData = new HashMap<>();
    private final PaperStatDisplayFeature.FormatRegistry statFormats = new PaperStatDisplayFeature.FormatRegistry(this);
    private final MapFont font = new MinecraftFont();
    private final Rule.Serializer ruleSerializer = new Rule.Serializer();
    private final StatMap.Serializer statMapSerializer = new StatMap.Serializer();
    private final PaperFeatureInstance.Serializer featureSerializer = new PaperFeatureInstance.Serializer(this);

    @Override public Registry<PaperComponent> components() { return components; }
    @Override public Registry<PaperBlueprint> blueprints() { return blueprints; }
    public Registry<FeatureType.Keyed> featureTypes() { return featureTypes; }
    public SokolPersistence persistence() { return persistence; }
    public PaperStatDisplayFeature.FormatRegistry statFormats() { return statFormats; }
    public MapFont font() { return font; }
    public Rule.Serializer ruleSerializer() { return ruleSerializer; }
    public StatMap.Serializer statMapSerializer() { return statMapSerializer; }
    public PaperFeatureInstance.Serializer featureSerializer() { return featureSerializer; }

    public PlayerData playerData(Player player) {
        return playerData.computeIfAbsent(player, p -> new PlayerData(this, p));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        featureTypes.register(DummyFeature.TYPE);
        featureTypes.register(PaperStatDisplayFeature.TYPE);

        PaperStatDisplayFeature.Formats.registerAll(statFormats);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            private void onEvent(PlayerQuitEvent event) {
                playerData.remove(event.getPlayer());
            }
        }, this);

        paperScheduler.run(Task.repeating(ctx -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                PlayerData data = playerData(player);
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (data) {
                    ctx.run(Task.single(data::paperTick));
                }
            }
        }, Ticks.MSPT));
        threadScheduler.run(Task.repeating(ctx -> {
            for (var data : playerData.values()) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (data) {
                    ctx.run(Task.single(data::threadTick));
                }
            }
        }, 10));
    }

    @Override
    public void onDisable() {
        threadScheduler.cancel();
    }

    @Override
    protected void configOptionsDefaults(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {
        super.configOptionsDefaults(serializers, mapperFactory);
        serializers
                .registerExact(NodePath.class, new NodePath.Serializer())
                .registerExact(Rule.class, ruleSerializer)
                .registerExact(StatIntermediate.Priority.class, new StatIntermediate.Priority.Serializer())
                .registerExact(StatMap.class, statMapSerializer)
                .registerExact(StatIntermediate.class, new StatIntermediate.Serializer())

                .registerExact(PaperComponent.class, new PaperComponent.Serializer(this))
                .registerExact(PaperFeatureInstance.class, featureSerializer)
                .registerExact(PaperNode.class, new PaperNode.Serializer(this))
                .registerExact(PaperBlueprint.class, new PaperBlueprint.Serializer(this))

                .registerExact(new TypeToken<StatDisplayFeature.Format<?>>(){}, new PaperStatDisplayFeature.FormatSerializer(statFormats));
    }

    private <T extends Keyed> void loadRegistry(String root, Class<T> type, Registry<T> registry) {
        String typeName = type.getSimpleName();
        registry.unregisterAll();
        Files.recursively(file(root), (file, path) -> {
            if (!FILE_EXTENSION.equals(Files.extension(file.getName())))
                return;
            ConfigurationNode node;
            try {
                node = loader(file).load();
            } catch (ConfigurateException e) {
                log(Logging.Level.WARNING, e, "Could not load any %s from %s", typeName, path);
                return;
            }

            for (var entry : node.node("entries").childrenMap().entrySet()) {
                T object;
                try {
                    object = entry.getValue().get(type);
                    if (object == null)
                        throw new NullPointerException("Null object deserialized");
                } catch (SerializationException e) {
                    log(Logging.Level.WARNING, e, "Could not load %s from /%s", typeName, path);
                    continue;
                } catch (Exception e) {
                    log(Logging.Level.WARNING, e, "Could not load %s from /%s @ %s", typeName, path, entry.getValue().path());
                    continue;
                }
                registry.register(object);
            }
        });

        for (var elem : registry.values())
            log(Logging.Level.VERBOSE, "Registered %s %s", typeName, elem.id());
        log(Logging.Level.INFO, "Loaded %d %s(s)", registry.size(), typeName);
    }

    @Override
    public void load() {
        super.load();
        if (setting(true, ConfigurationNode::getBoolean, "enable_bstats")) {
            Metrics metrics = new Metrics(this, BSTATS_ID);
        }

        for (var entry : settings.root().node("font").childrenMap().entrySet()) {
            String key = ""+entry.getKey();
            if (key.length() != 1) {
                log(Logging.Level.WARNING, "Key in font map must be 1 character");
                continue;
            }
            int width = entry.getValue().getInt();
            font.setChar(key.charAt(0), new MapFont.CharacterSprite(width, 0, new boolean[0]));
        }

        loadRegistry(PATH_COMPONENT, PaperComponent.class, components);
        loadRegistry(PATH_BLUEPRINT, PaperBlueprint.class, blueprints);
    }

    @Override
    public SokolCommand createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
