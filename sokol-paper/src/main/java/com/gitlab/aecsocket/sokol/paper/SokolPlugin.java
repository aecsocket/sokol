package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Files;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.Registry;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.core.system.SlotInfoSystem;
import com.gitlab.aecsocket.sokol.paper.system.PaperItemSystem;
import com.gitlab.aecsocket.sokol.paper.system.PaperSlotInfoSystem;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.ItemDescriptor;
import io.leangen.geantyref.TypeToken;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.Map;

public class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform {
    public static final String FILE_EXTENSION = "conf";
    public static final String PATH_COMPONENT = "component";

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<PaperSystem.KeyedType> systemTypes = new Registry<>();
    private final PersistenceManager persistenceManager = new PersistenceManager(this);
    private final SlotViewGuis slotViewGuis = new SlotViewGuis(this);
    private final StatMap.Serializer statMapSerializer = new StatMap.Serializer();
    private final PaperSystem.Serializer systemSerializer = new PaperSystem.Serializer(this);
    private final ItemDescriptor invalidItem = new ItemDescriptor(this, Material.BARRIER, 0, 0, false);

    @Override
    public void onEnable() {
        super.onEnable();
        registerSystemType(ItemSystem.ID, PaperItemSystem.TYPE);
        registerSystemType(SlotInfoSystem.ID, PaperSlotInfoSystem.TYPE);
    }

    @Override public Registry<PaperComponent> components() { return components; }
    public Registry<PaperSystem.KeyedType> systemTypes() { return systemTypes; }
    public PersistenceManager persistenceManager() { return persistenceManager; }
    public SlotViewGuis slotViewGuis() { return slotViewGuis; }
    public StatMap.Serializer statMapSerializer() { return statMapSerializer; }
    public PaperSystem.Serializer systemSerializer() { return systemSerializer; }
    public ItemDescriptor invalidItem() { return invalidItem; }

    @Override public PaperComponent component(String id) { return components.get(id); }

    public void registerSystemType(String id, PaperSystem.Type type) {
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

    @Override
    protected void configOptionsDefaults(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {
        super.configOptionsDefaults(serializers, mapperFactory);
        serializers.register(StatMap.Priority.class, new StatMap.Priority.Serializer());
        serializers.register(StatMap.class, statMapSerializer);
        serializers.register(StatLists.class, new StatLists.Serializer());
        serializers.register(ItemDescriptor.class, new ItemDescriptor.Serializer(this));
        serializers.register(PaperSlot.class, new PaperSlot.Serializer(this));
        serializers.register(PaperComponent.class, new PaperComponent.Serializer(this));
        serializers.register(PaperSystem.Instance.class, systemSerializer);
        serializers.register(PaperTreeNode.class, new PaperTreeNode.Serializer(this));
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
