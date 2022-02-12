package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.minecommons.core.Logging;
import com.github.aecsocket.minecommons.paper.effect.PaperEffectors;
import com.github.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.github.aecsocket.minecommons.paper.plugin.BasePlugin;

import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.registry.Registry;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatMap;
import com.github.aecsocket.sokol.paper.context.PaperContext;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import com.github.aecsocket.sokol.paper.world.slot.PaperItemSlot;

import io.leangen.geantyref.TypeToken;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public final class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform.Scoped<PaperComponent, PaperFeature> {
    public static final String
        CONFIG_EXTENSION = "conf",
        PATH_COMPONENT = "component",
        PATH_BLUEPRINT = "blueprint";
    public static final int BSTATS_ID = 11870;

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<PaperFeature> features = new Registry<>();
    private final PaperEffectors effectors = new PaperEffectors(this);
    private final SokolPersistence persistence = new SokolPersistence(this);
    private final MapFont font = new MinecraftFont();

    private final StatMap.Serializer statsSerializer = new StatMap.Serializer();
    private final Rule.Serializer rulesSerializer = new Rule.Serializer();

    @Override public Registry<PaperComponent> components() { return components; }
    @Override public Registry<PaperFeature> features() { return features; }
    public PaperEffectors effectors() { return effectors; }
    public SokolPersistence persistence() { return persistence; }
    public MapFont font() { return font; }

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                PaperItemUser.OfPlayer user = PaperItemUser.user(this, player);
                PlayerInventory inventory = player.getInventory();
                for (var slot : EquipmentSlot.values()) {
                    ItemStack item = inventory.getItem(slot);
                    persistence.load(item).ifPresent(bp ->
                        bp.asTreeNode(PaperContext.context(
                            user,
                            new PaperItemStack(this, item),
                            PaperItemSlot.itemSlot(this, player, slot)
                        )).tree().andCall(PaperEvents.Hold::new)
                    );
                }
            }
        }, 0, 1);
    }

    @Override
    protected void configOptionsDefaults(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {
        super.configOptionsDefaults(serializers, mapperFactory);
        serializers
            .register(StatMap.class, statsSerializer)
            .register(Rule.class, rulesSerializer)
            .register(StatIntermediate.class, new StatIntermediate.Serializer())
            .register(StatIntermediate.Priority.class, new StatIntermediate.Priority.Serializer())

            .register(PaperComponent.class, new PaperComponent.Serializer(this))
            .register(PaperBlueprintNode.class, new PaperBlueprintNode.Serializer(this));
    }

    @Override
    public void setUpSerializers(Map<String, Stat<?>> statTypes, Map<String, Class<? extends Rule>> ruleTypes) {
        statsSerializer.types(statTypes);
        rulesSerializer.types(ruleTypes);
    }

    @Override
    public void tearDownSerializers() {
        statsSerializer.types(null);
        rulesSerializer.types(null);
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

        loadRegistry(path(PATH_COMPONENT), PaperComponent.class.getSimpleName(), new TypeToken<PaperComponent>() {}, components);
        // TODO keyed ver of BPs
        //loadRegistry(PATH_BLUEPRINT, PaperBlueprint.class.getSimpleName(), new TypeToken<KeyedBlueprint<PaperBlueprint>>() {}, blueprints);
    }

    private <T extends Keyed> void loadRegistry(Path root, String typeName, TypeToken<T> type, Registry<T> registry) {
        registry.unregisterAll();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    if (!path.toString().endsWith(CONFIG_EXTENSION))
                        return FileVisitResult.CONTINUE;
                    ConfigurationNode node;
                    try {
                        node = loaderBuilder().path(path).build().load();
                    } catch (ConfigurateException e) {
                        log(Logging.Level.WARNING, e, "Could not load any %s from /%s", typeName, path);
                        return FileVisitResult.CONTINUE;
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
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) {
                    log(Logging.Level.WARNING, e, "Could not load %s from /%s", typeName, path);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log(Logging.Level.ERROR, e, "Could not open %s for reading", root);
        }

        for (var elem : registry.values())
            log(Logging.Level.VERBOSE, "Registered %s %s", typeName, elem.id());
        log(Logging.Level.INFO, "Loaded %d %s(s)", registry.size(), typeName);
    }

    @Override
    protected BaseCommand<SokolPlugin> createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
