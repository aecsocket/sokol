package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Files;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.minecommons.core.Validation;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.Blueprint;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.Registry;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.core.system.SlotInfoSystem;
import com.gitlab.aecsocket.sokol.paper.stat.Descriptor;
import com.gitlab.aecsocket.sokol.paper.system.SlotsSystem;
import com.gitlab.aecsocket.sokol.paper.system.impl.PaperItemSystem;
import com.gitlab.aecsocket.sokol.paper.system.impl.PaperSlotInfoSystem;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.ItemDescriptor;
import io.leangen.geantyref.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sokol's main plugin class. Use {@link #instance()} to get the singleton instance.
 */
public class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform {
    /**
     * A function which sets up the serializers and object mapper factories, when making the config options.
     */
    @FunctionalInterface
    public interface ConfigOptionInitializer {
        /**
         * Runs before default Sokol serializers are registered.
         * @param serializers The serializers.
         * @param mapperFactory The object mapper factory builder.
         */
        default void pre(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {}

        /**
         * Runs after default Sokol serializers are registered.
         * @param serializers The serializers.
         * @param mapperFactory The object mapper factory builder.
         */
        void post(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory);
    }

    /** The file extension required for files to have, so that they are loaded. */
    public static final String FILE_EXTENSION = "conf";
    /** The path, from the plugin's data folder, from which components will be loaded. */
    public static final String PATH_COMPONENT = "component";

    /**
     * Gets the singleton instance of this plugin.
     * @return The plugin.
     */
    public static SokolPlugin instance() { return getPlugin(SokolPlugin.class); }

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<Blueprint> blueprints = new Registry<>();
    private final Registry<PaperSystem.KeyedType> systemTypes = new Registry<>();
    private final List<ConfigOptionInitializer> configOptionInitializers = new ArrayList<>();
    private final PersistenceManager persistenceManager = new PersistenceManager(this);
    private final SlotViewGuis slotViewGuis = new SlotViewGuis(this);
    private final StatMap.Serializer statMapSerializer = new StatMap.Serializer();
    private final Rule.Serializer ruleSerializer = new Rule.Serializer();
    private final PaperSystem.Serializer systemSerializer = new PaperSystem.Serializer(this);
    private final ItemDescriptor invalidItem = new ItemDescriptor(this, Material.BARRIER, 0, 0, false);

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(new SokolListener(this), this);
        registerSystemType(ItemSystem.ID, PaperItemSystem.TYPE);
        registerSystemType(SlotInfoSystem.ID, PaperSlotInfoSystem.TYPE);
        registerSystemType(SlotsSystem.ID, SlotsSystem.TYPE);
    }

    @Override public @NotNull Registry<PaperComponent> components() { return components; }
    @Override public @NotNull Registry<Blueprint> blueprints() { return blueprints; }
    public @NotNull Registry<PaperSystem.KeyedType> systemTypes() { return systemTypes; }
    public @NotNull PersistenceManager persistenceManager() { return persistenceManager; }
    public @NotNull SlotViewGuis slotViewGuis() { return slotViewGuis; }
    public @NotNull StatMap.Serializer statMapSerializer() { return statMapSerializer; }
    public @NotNull Rule.Serializer ruleSerializer() { return ruleSerializer; }
    public @NotNull PaperSystem.Serializer systemSerializer() { return systemSerializer; }
    public @NotNull ItemDescriptor invalidItem() { return invalidItem; }

    /**
     * Registers a system type, which is looked up when deserializing the data files.
     * @param id The system's ID.
     * @param type The system creator.
     */
    public void registerSystemType(String id, PaperSystem.Type type) {
        Validation.notNull(id, "id");
        Validation.notNull(type, "type");
        if (!Keyed.validKey(id))
            throw new IllegalArgumentException("Invalid system ID [" + id + "], must be " + Keyed.VALID_KEY);
        systemTypes.put(id, new PaperSystem.KeyedType() {
            @Override public @NotNull String id() { return id; }
            @Override
            public PaperSystem create(SokolPlugin plugin, ConfigurationNode node) throws SerializationException {
                return type.create(plugin, node);
            }
        });
    }

    /**
     * Adds a function to run when setting up the config options.
     * <p>
     * This should be used to inject custom type serializers and object mapper settings.
     * @param init The function to apply.
     */
    public void configOptionInitializer(ConfigOptionInitializer init) {
        configOptionInitializers.add(init);
    }

    @Override
    protected void configOptionsDefaults(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {
        super.configOptionsDefaults(serializers, mapperFactory);
        configOptionInitializers.forEach(i -> i.pre(serializers, mapperFactory));
        serializers.register(StatMap.Priority.class, new StatMap.Priority.Serializer());
        serializers.register(StatMap.class, statMapSerializer);
        serializers.register(StatLists.class, new StatLists.Serializer());
        serializers.register(ItemDescriptor.class, new ItemDescriptor.Serializer(this));
        serializers.register(PaperSlot.class, new PaperSlot.Serializer(this));
        serializers.register(PaperComponent.class, new PaperComponent.Serializer(this));
        serializers.registerExact(Rule.class, ruleSerializer);
        serializers.register(PaperSystem.Instance.class, systemSerializer);
        serializers.register(PaperTreeNode.class, new PaperTreeNode.Serializer(this));
        serializers.register(new TypeToken<Descriptor<List<PreciseSound>>>() {}, new Descriptor.Serializer<>(new TypeToken<List<PreciseSound>>() {}));
        configOptionInitializers.forEach(i -> i.post(serializers, mapperFactory));
    }

    @Override
    public boolean load() {
        if (super.load()) {
            components.clear();

            Files.recursively(file(PATH_COMPONENT), (file, path) -> {
                if (!FILE_EXTENSION.equals(Files.extension(file.getName()))) return;
                try {
                    Map<?, PaperComponent> loaded = loader(file).load().node("entries").get(new TypeToken<Map<String, PaperComponent>>() {});
                    if (loaded == null)
                        return;
                    for (PaperComponent o : loaded.values()) {
                        components.register(o);
                    }
                } catch (ConfigurateException e) {
                    log(Logging.Level.WARNING, e, "Could not load items from %s", path);
                }
            });

            for (Component o : components.values()) {
                log(Logging.Level.VERBOSE, "Registered component [%s]", o.id());
            }
            log(Logging.Level.INFO, "Registered %d objects", components.size());
            return true;
        }
        return false;
    }

    @Override
    protected BaseCommand<SokolPlugin> createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
