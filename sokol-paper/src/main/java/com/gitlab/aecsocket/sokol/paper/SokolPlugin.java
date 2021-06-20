package com.gitlab.aecsocket.sokol.paper;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.gitlab.aecsocket.minecommons.core.Files;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform {
    public static final String FILE_EXTENSION = "conf";
    public static final String PATH_COMPONENT = "component";

    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<PaperSystem.KeyedType> systemTypes = new Registry<>();
    private final PersistenceManager persistenceManager = new PersistenceManager(this);
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

    private void updateGui(ChestGui gui) {
        Map<HumanEntity, ItemStack> cursors = new HashMap<>();
        for (HumanEntity human : gui.getViewers()) {
            cursors.put(human, human.getItemOnCursor());
            human.setItemOnCursor(null);
        }
        gui.update();
        for (var entry : cursors.entrySet()) {
            entry.getKey().setItemOnCursor(entry.getValue());
        }
    }

    public ChestGui createSlotViewGui(PaperTreeNode tree, Locale locale, Consumer<SlotViewPane> paneFunction) {
        // todo components
        ChestGui gui = new ChestGui(6, LegacyComponentSerializer.legacySection().serialize(tree.value().name(locale)));
        SlotViewPane pane = new SlotViewPane(this, 9, 6, locale, tree)
                .treeModifyCallback(() -> Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> updateGui(gui)));
        paneFunction.accept(pane);
        gui.addPane(pane);
        gui.setOnGlobalClick(event -> {
            ItemStack clicked = event.getCurrentItem();
            PaperTreeNode clickedNode = persistenceManager.load(clicked);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                if (PaperUtils.empty(clicked))
                    event.setCancelled(true);
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                    return;
                }
                if (!persistenceManager.isTree(event.getCursor()) && clickedNode == null)
                    return;

                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    pane.updateItems(persistenceManager.load(event.getCursor()));
                    updateGui(gui);
                });
            }
        });
        gui.setOnTopDrag(event -> event.setCancelled(true));
        return gui;
    }
}
