package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.*;
import com.gitlab.aecsocket.minecommons.core.scheduler.Task;
import com.gitlab.aecsocket.minecommons.core.scheduler.ThreadScheduler;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.display.Particles;
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.minecommons.paper.inputs.Inputs;
import com.gitlab.aecsocket.minecommons.paper.inputs.ListenerInputs;
import com.gitlab.aecsocket.minecommons.paper.inputs.PacketInputs;
import com.gitlab.aecsocket.minecommons.paper.persistence.StringArrayType;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.Registry;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Descriptor;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat;
import com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat;
import com.gitlab.aecsocket.sokol.core.stat.inbuilt.VectorStat;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.ItemSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.NodeHolderSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SlotInfoSystem;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.paper.stat.*;
import com.gitlab.aecsocket.sokol.paper.system.inbuilt.*;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.Animation;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import io.leangen.geantyref.TypeToken;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import static com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser.*;
import static com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperSlot.*;

/**
 * Sokol's main plugin class. Use {@link #instance()} to get the singleton instance.
 */
public class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform {
    /** The ID for this plugin on https://bstats.org. */
    public static final int BSTATS_ID = 11870;

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
    /** The path, from the plugin's data folder, from which blueprints will be loaded. */
    public static final String PATH_BLUEPRINT = "blueprint";

    /**
     * Gets the singleton instance of this plugin.
     * @return The plugin.
     */
    public static SokolPlugin instance() { return getPlugin(SokolPlugin.class); }

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<PaperBlueprint> blueprints = new Registry<>();
    private final Registry<PaperSystem.KeyedType> systemTypes = new Registry<>();
    private final List<ConfigOptionInitializer> configOptionInitializers = new ArrayList<>();
    private final PersistenceManager persistenceManager = new PersistenceManager(this);
    private final PaperScheduler paperScheduler = new PaperScheduler(this);
    private final ThreadScheduler threadScheduler = new ThreadScheduler(Executors.newSingleThreadExecutor());
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private final SchedulerSystem.GlobalScheduler<PaperEvent.Hold> systemScheduler = SchedulerSystem.GlobalScheduler.create();
    private final Guis guis = new Guis(this);
    private final PacketInputs packetInputs = new PacketInputs(this);
    private final ListenerInputs listenerInputs = new ListenerInputs();
    private final SokolPacketListener packetListener = new SokolPacketListener(this);
    private final StatMap.Serializer statMapSerializer = new StatMap.Serializer();
    private final Rule.Serializer ruleSerializer = new Rule.Serializer();
    private final PaperSystem.Serializer systemSerializer = new PaperSystem.Serializer(this);
    private final ItemDescriptor invalidItem = ItemDescriptor.of(this, Material.BARRIER, 0, 0, false);
    private final PersistentDataType<PersistentDataContainer, SystemPath> typeSystemPath = new PersistentDataType<>() {
        @Override public @NotNull Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
        @Override public @NotNull Class<SystemPath> getComplexType() { return SystemPath.class; }

        @Override
        public @NotNull PersistentDataContainer toPrimitive(@NotNull SystemPath obj, @NotNull PersistentDataAdapterContext ctx) {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            data.set(key("system"), PersistentDataType.STRING, obj.system());
            data.set(key("nodes"), StringArrayType.INSTANCE, obj.nodes());
            return data;
        }

        @Override
        public @NotNull SystemPath fromPrimitive(@NotNull PersistentDataContainer obj, @NotNull PersistentDataAdapterContext ctx) {
            return SystemPath.path(
                    obj.get(key("system"), PersistentDataType.STRING),
                    obj.get(key("nodes"), StringArrayType.INSTANCE)
            );
        }
    };

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(new SokolListener(this), this);
        protocol.manager().addPacketListener(packetListener);

        paperScheduler.run(Task.repeating(ctx -> {
            for (var data : playerData.values()) {
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

        protocol.manager().addPacketListener(packetInputs);
        packetInputs.events().register(Inputs.Events.Input.class, event -> {
            Player player = event.player();
            PlayerUser user = player(this, player);
            callItemEvent(player, EquipmentSlot.HAND, (node, slot) ->
                    new PaperEvent.RawInputEvent(node, user, equip(this, player, slot), event));
            callItemEvent(player, EquipmentSlot.OFF_HAND, (node, slot) ->
                    new PaperEvent.RawInputEvent(node, user, equip(this, player, slot), event));

            if (event.input() == InputType.DROP)
                // drops are handled by listenerInputs
                return;
            handlePacketInput(player, user, event, EquipmentSlot.HAND);
            handlePacketInput(player, user, event, EquipmentSlot.OFF_HAND);
            if (event.input() == InputType.SWAP && !event.cancelled())
                playerData(player).stopAnimation();
        });

        Bukkit.getPluginManager().registerEvents(listenerInputs, this);
        listenerInputs.events().register(Inputs.Events.Input.class, event -> {
            Player player = event.player();
            PlayerUser user = player(this, player);
            callItemEvent(player, EquipmentSlot.HAND, (node, slot) ->
                    new PaperEvent.RawInputEvent(node, user, equip(this, player, slot), event));
            callItemEvent(player, EquipmentSlot.OFF_HAND, (node, slot) ->
                    new PaperEvent.RawInputEvent(node, user, equip(this, player, slot), event));
            if (event.input() != InputType.DROP)
                // other event types are handled by packetInputs
                return;
            if (event instanceof ListenerInputs.Events.DropInput drop) {
                Item itemDrop = drop.event().getItemDrop();
                persistenceManager.load(itemDrop.getItemStack()).ifPresent(node -> {
                    if (new PaperEvent.Input(node,
                            player(this, player),
                            slot(this, itemDrop::getItemStack, itemDrop::setItemStack),
                            event).call())
                        event.cancel();
                });
            }
        });

        registerSystemType(ItemSystem.ID, PaperItemSystem.type(this), () -> PaperItemSystem.LOAD_PROVIDER);
        registerSystemType(SlotInfoSystem.ID, PaperSlotInfoSystem.type(this), () -> PaperSlotInfoSystem.LOAD_PROVIDER);
        registerSystemType(SchedulerSystem.ID, PaperSchedulerSystem.type(this), () -> PaperSchedulerSystem.LOAD_PROVIDER);
        registerSystemType(NodeHolderSystem.ID, PaperNodeHolderSystem.type(this), () -> PaperNodeHolderSystem.LOAD_PROVIDER);
        registerSystemType(SlotsSystem.ID, SlotsSystem.type(this), () -> SlotsSystem.LOAD_PROVIDER);
        registerSystemType(PropertiesSystem.ID, PropertiesSystem.type(this), () -> PropertiesSystem.LOAD_PROVIDER);
    }

    @Override
    public void onDisable() {
        paperScheduler.cancel();
        threadScheduler.cancel();
    }

    private void callItemEvent(Player player, EquipmentSlot slot, BiFunction<PaperTreeNode, EquipmentSlot, TreeEvent> event) {
        persistenceManager.load(player.getInventory().getItem(slot)).ifPresent(node -> event.apply(node, slot).call());
    }

    private void handlePacketInput(Player player, PlayerUser user, Inputs.Events.Input event, EquipmentSlot slot) {
        persistenceManager.load(player.getInventory().getItem(slot)).ifPresent(node -> {
            if (new PaperEvent.Input(node,
                    user,
                    equip(this, player, slot),
                    event).call())
                event.cancel();
        });
    }

    @Override public Registry<PaperComponent> components() { return components; }
    @Override public Registry<PaperBlueprint> blueprints() { return blueprints; }
    public Registry<PaperSystem.KeyedType> systemTypes() { return systemTypes; }
    public PersistenceManager persistenceManager() { return persistenceManager; }
    public PlayerData playerData(Player player) { return playerData.computeIfAbsent(player.getUniqueId(), u -> new PlayerData(this, player)); }
    public SchedulerSystem.GlobalScheduler<PaperEvent.Hold> systemScheduler() { return systemScheduler; }
    public Guis guis() { return guis; }
    public PacketInputs packetInputs() { return packetInputs; }
    public ListenerInputs listenerInputs() { return listenerInputs; }
    public SokolPacketListener packetListener() { return packetListener; }
    public StatMap.Serializer statMapSerializer() { return statMapSerializer; }
    public Rule.Serializer ruleSerializer() { return ruleSerializer; }
    public PaperSystem.Serializer systemSerializer() { return systemSerializer; }
    public ItemDescriptor invalidItem() { return invalidItem; }
    public PersistentDataType<PersistentDataContainer, SystemPath> typeSystemPath() { return typeSystemPath; }

    /**
     * Registers a system type, which is looked up when deserializing the data files.
     * @param id The system's ID.
     * @param configType The system creator, for creating systems on deserialization.
     * @param loadProviderType The load provider creator, for adding load-systems on deserialization, but not creating a system.
     */
    public SokolPlugin registerSystemType(String id, PaperSystem.ConfigType configType, PaperSystem.LoadProviderType loadProviderType) {
        Validation.notNull("id", id);
        Validation.notNull("configType", configType);
        Validation.notNull("loadProviderType", loadProviderType);
        if (!Keyed.validKey(id))
            throw new IllegalArgumentException("Invalid system ID [" + id + "], must match " + Keyed.VALID_KEY);
        systemTypes.put(id, new PaperSystem.KeyedType() {
            @Override
            public String id() { return id; }

            @Override
            public PaperSystem createSystem(ConfigurationNode cfg) throws SerializationException {
                return configType.createSystem(cfg);
            }

            @Override
            public LoadProvider createLoadProvider() {
                return loadProviderType.createLoadProvider();
            }
        });
        return this;
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
        serializers
                .register(SystemPath.class, SystemPath.Serializer.INSTANCE)
                .register(InputMapper.class, InputMapper.Serializer.INSTANCE)
                .register(Animation.class, Animation.Serializer.INSTANCE)
                .register(Animation.Frame.class, Animation.Frame.Serializer.INSTANCE)
                .register(Animation.NumberProperty.class, Animation.NumberProperty.Serializer.INSTANCE)
                .register(StatMap.Priority.class, new StatMap.Priority.Serializer())
                .register(StatMap.class, statMapSerializer)
                .register(StatLists.class, new StatLists.Serializer())
                .register(ItemDescriptor.class, new ItemDescriptor.Serializer(this))
                .register(PaperSlot.class, new PaperSlot.Serializer(this))
                .register(PaperComponent.class, new PaperComponent.Serializer(this))
                .registerExact(Rule.class, ruleSerializer)
                .register(PaperSystem.Instance.class, systemSerializer)
                .register(PaperTreeNode.class, new PaperTreeNode.Serializer(this))
                .register(PaperBlueprint.class, new PaperBlueprint.Serializer(this))

                .register(new TypeToken<Descriptor<Boolean>>() {}, PrimitiveStat.SBoolean.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<Integer>>() {}, PrimitiveStat.SInteger.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<Long>>() {}, PrimitiveStat.SLong.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<Float>>() {}, PrimitiveStat.SFloat.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<Double>>() {}, PrimitiveStat.SDouble.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<String>>() {}, StringStat.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<Vector2>>() {}, VectorStat.SVector2.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<Vector3>>() {}, VectorStat.SVector3.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<Animation>>() {}, AnimationStat.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<ItemDescriptor>>() {}, ItemStat.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<List<PreciseSound>>>() {}, SoundsStat.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<List<Particles>>>() {}, ParticlesStat.Serializer.INSTANCE)
                .register(new TypeToken<Descriptor<List<PotionEffect>>>() {}, EffectsStat.Serializer.INSTANCE);
        configOptionInitializers.forEach(i -> i.post(serializers, mapperFactory));
    }

    private <T extends Keyed> void loadRegistry(String root, Class<T> type, Registry<T> registry) {
        String name = type.getSimpleName();
        registry.clear();
        Files.recursively(file(root), (file, path) -> {
            if (!FILE_EXTENSION.equals(Files.extension(file.getName()))) return;
            ConfigurationNode node;
            try {
                node = loader(file).load();
            } catch (ConfigurateException e) {
                log(Logging.Level.WARNING, e, "Could not load %s objects from %s", name, path);
                return;
            }

            for (var entry : node.node("entries").childrenMap().entrySet()) {
                T object;
                try {
                    object = entry.getValue().get(type);
                } catch (Exception e) {
                    log(Logging.Level.WARNING, e, "Could not load %s at %s", name, entry.getValue().path(), path);
                    continue;
                }
                if (object != null)
                    registry.register(object);
            }
        });

        for (T o : registry.values()) {
            log(Logging.Level.VERBOSE, "Registered %s [%s]", name, o.id());
        }
        log(Logging.Level.INFO, "Registered %d of %s", registry.size(), name);
    }

    void remove(Player player) {
        playerData.remove(player.getUniqueId());
    }

    @Override
    public void load() {
        super.load();
        if (setting(true, ConfigurationNode::getBoolean, "enable_bstats")) {
            Metrics metrics = new Metrics(this, BSTATS_ID);
        }

        loadRegistry(PATH_COMPONENT, PaperComponent.class, components);
        loadRegistry(PATH_BLUEPRINT, PaperBlueprint.class, blueprints);
    }

    @Override
    protected BaseCommand<SokolPlugin> createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
