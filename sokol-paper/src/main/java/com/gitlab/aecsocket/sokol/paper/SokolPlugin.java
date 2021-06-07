package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Files;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.registry.Registry;
import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.Map;

public class SokolPlugin extends BasePlugin<SokolPlugin> {
    public static final String FILE_EXTENSION = "conf";
    public static final String PATH_COMPONENT = "component";

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<PaperSystem.KeyedType> systemTypes = new Registry<>();
    private final ItemManager itemManager = new ItemManager(this);

    @Override
    public void onEnable() {
        super.onEnable();
    }

    public Map<String, PaperComponent> components() { return components; }
    public Map<String, PaperSystem.KeyedType> systemTypes() { return systemTypes; }
    public ItemManager itemManager() { return itemManager; }

    public void registerSystemType(String id, PaperSystem.Type type) {
        systemTypes.put(id, new PaperSystem.KeyedType() {
            @Override public @NotNull String id() { return id; }
            @Override public PaperSystem create(ConfigurationNode node) throws SerializationException { return type.create(node); }
        });
    }

    @Override
    protected void configOptionsDefaults(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {
        super.configOptionsDefaults(serializers, mapperFactory);
        serializers.register(PaperComponent.class, new PaperComponent.Serializer(this));
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
