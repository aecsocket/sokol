package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Files;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.Registry;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.paper.impl.PaperBlueprint;
import com.gitlab.aecsocket.sokol.paper.impl.PaperComponent;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

public class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform {
    public static final String FILE_EXTENSION = "conf";
    public static final String PATH_COMPONENT = "component";
    public static final String PATH_BLUEPRINT = "blueprint";

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<PaperBlueprint> blueprints = new Registry<>();
    private final Rule.Serializer ruleSerializer = new Rule.Serializer();
    private final StatMap.Serializer statMapSerializer = new StatMap.Serializer();

    @Override public Registry<PaperComponent> components() { return components; }
    @Override public Registry<PaperBlueprint> blueprints() { return blueprints; }
    public Rule.Serializer ruleSerializer() { return ruleSerializer; }
    public StatMap.Serializer statMapSerializer() { return statMapSerializer; }

    @Override
    protected void configOptionsDefaults(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {
        super.configOptionsDefaults(serializers, mapperFactory);
        serializers
                .registerExact(NodePath.class, new NodePath.Serializer())
                .registerExact(Rule.class, ruleSerializer)
                .registerExact(StatIntermediate.Priority.class, new StatIntermediate.Priority.Serializer())
                .registerExact(StatMap.class, statMapSerializer)
                .registerExact(StatIntermediate.class, new StatIntermediate.Serializer());
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
                } catch (Exception e) {
                    log(Logging.Level.WARNING, e, "Could not load %s at %s @ %s", typeName, entry.getValue().path(), path);
                    continue;
                }
                registry.register(object);
            }
        });

        for (var elem : registry.values())
            log(Logging.Level.VERBOSE, "Registered %s %s", typeName, elem.id());
        log(Logging.Level.INFO, "Registered %dx %s", registry.size(), typeName);
    }

    @Override
    public void load() {
        super.load();
        log(Logging.Level.INFO, "Loading components");
        loadRegistry(PATH_COMPONENT, PaperComponent.class, components);
        log(Logging.Level.INFO, "Loading blueprints");
        loadRegistry(PATH_BLUEPRINT, PaperBlueprint.class, blueprints);
    }

    @Override
    public SokolCommand createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
