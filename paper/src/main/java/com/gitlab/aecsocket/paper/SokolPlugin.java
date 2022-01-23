package com.gitlab.aecsocket.paper;

import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.impl.KeyedBlueprint;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.registry.Registry;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.rule.node.NodeRuleTypes;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.core.stat.StatMap;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.minecommons.core.Files;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.minecommons.paper.effect.PaperEffectors;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.paper.context.PaperContext;
import com.gitlab.aecsocket.paper.impl.*;
import com.gitlab.aecsocket.paper.world.PaperItemUser;
import com.gitlab.aecsocket.paper.world.slot.PaperItemSlot;

import io.leangen.geantyref.TypeToken;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;
import org.bukkit.scheduler.BukkitRunnable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.List;
import java.util.Map;

public final class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform.Scoped<PaperComponent, PaperBlueprint, PaperFeature> {
    public static final String CONFIG_EXTENSION = "conf";
    public static final String PATH_COMPONENT = "component";
    public static final String PATH_BLUEPRINT = "blueprint";
    public static final int BSTATS_ID = 11870;

    private static SokolPlugin instance;

    /**
     * Gets the global instance of this plugin.
     * @return The instance.
     */
    public static SokolPlugin instance() { return instance; }

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<KeyedBlueprint<PaperBlueprint>> blueprints = new Registry<>();
    private final Registry<PaperFeature> features = new Registry<>();
    private final SokolPersistence persistence = new SokolPersistence(this);
    private final MapFont font = new MinecraftFont();
    private final PaperEffectors effectors = new PaperEffectors(this);
    private final StatMap.Serializer statMapSerializer = new StatMap.Serializer();

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                PaperItemUser.OfPlayer user = PaperItemUser.user(this, player);
                PlayerInventory inv = player.getInventory();
                for (var slot : EquipmentSlot.values()) {
                    ItemStack item = inv.getItem(slot);
                    //noinspection ConstantConditions
                    persistence.safeLoad(item).ifPresent(bp -> bp
                            .asNode(PaperContext.context(user, new PaperItemStack(this, item), PaperItemSlot.itemSlot(this, player, slot)))
                            .tree().andCall(PaperItemEvent.Hold::new));
                }
            }
        }, 0, 1);
    }

    @Override public Registry<PaperComponent> components() { return components; }
    @Override public Registry<KeyedBlueprint<PaperBlueprint>> blueprints() { return blueprints; }
    @Override public Registry<PaperFeature> features() { return features; }
    public SokolPersistence persistence() { return persistence; }
    public MapFont font() { return font; }
    public PaperEffectors effectors() { return effectors; }

    @Override
    public void setUpSerializers(Map<String, Stat<?>> statTypes, Map<String, Class<? extends NodeRule>> ruleTypes) {
        statMapSerializer.types(statTypes);
    }

    @Override
    public void tearDownSerializers() {
        statMapSerializer.types(null);
    }

    @Override
    protected void configOptionsDefaults(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {
        super.configOptionsDefaults(serializers, mapperFactory);
        serializers
                .registerExact(new TypeToken<KeyedBlueprint<PaperBlueprint>>() {}, new KeyedBlueprint.Serializer<>(this, PaperBlueprint.class));
    }

    private <T extends Keyed> void loadRegistry(String root, String typeName, TypeToken<T> type, Registry<T> registry) {
        registry.unregisterAll();
        Files.recursively(file(root), (file, path) -> {
            if (!file.getName().endsWith(CONFIG_EXTENSION))
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

        loadRegistry(PATH_COMPONENT, PaperComponent.class.getSimpleName(), new TypeToken<PaperComponent>() {}, components);
        loadRegistry(PATH_BLUEPRINT, PaperBlueprint.class.getSimpleName(), new TypeToken<KeyedBlueprint<PaperBlueprint>>() {}, blueprints);
    }

    @Override
    protected SokolCommand createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
