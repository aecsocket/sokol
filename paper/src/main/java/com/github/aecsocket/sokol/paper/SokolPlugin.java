package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.minecommons.paper.effect.PaperEffectors;
import com.github.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.github.aecsocket.minecommons.paper.plugin.BasePlugin;

import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.registry.Registry;
import com.github.aecsocket.sokol.paper.context.PaperContext;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import com.github.aecsocket.sokol.paper.world.slot.PaperItemSlot;

import org.bukkit.Bukkit;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform.Scoped<PaperComponent, PaperFeature> {
    private final Registry<PaperComponent> components = new Registry<>();
    private final Registry<PaperFeature> features = new Registry<>();
    private final PaperEffectors effectors = new PaperEffectors(this);
    private final SokolPersistence persistence = new SokolPersistence(this);

    @Override public Registry<PaperComponent> components() { return components; }
    @Override public Registry<PaperFeature> features() { return features; }
    public PaperEffectors effectors() { return effectors; }
    public SokolPersistence persistence() { return persistence; }

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
                        )).tree().andCall(PaperItemEvent.Hold::new));
                }
            }
        }, 0, 1);
    }

    @Override
    protected BaseCommand<SokolPlugin> createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
