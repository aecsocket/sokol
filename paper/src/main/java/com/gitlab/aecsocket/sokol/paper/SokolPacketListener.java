package com.gitlab.aecsocket.sokol.paper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.EntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SokolPacketListener extends PacketAdapter {
    private final SokolPlugin plugin;
    private final NamespacedKey hideUpdateKey;

    public SokolPacketListener(SokolPlugin plugin) {
        super(plugin, PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.ENTITY_EQUIPMENT);
        this.plugin = plugin;
        hideUpdateKey = plugin.key("hide_update");
    }

    public SokolPlugin plugin() { return plugin; }

    private void flag(ItemStack item, NamespacedKey key) {
        PaperUtils.modify(item, meta -> meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 0));
    }

    private void unflag(ItemStack item, NamespacedKey key) {
        PaperUtils.modify(item, meta -> meta.getPersistentDataContainer().remove(key));
    }

    private boolean flagged(ItemStack item, NamespacedKey key) {
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public void hideUpdate(ItemStack item) { flag(item, hideUpdateKey); }
    public void showUpdate(ItemStack item) { unflag(item, hideUpdateKey); }
    public boolean updatesHidden(ItemStack item) { return flagged(item, hideUpdateKey); }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();

        if (type == PacketType.Play.Server.SET_SLOT) {
            ItemStack item = packet.getItemModifier().read(0);
            if (item.hasItemMeta() && updatesHidden(item)) {
                event.setCancelled(true);
            }
        }
        if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            EntityUser user = PaperUser.anyEntity(plugin, packet.getEntityModifier(event).read(0));
            for (var entry : packet.getSlotStackPairLists().read(0)) {
                if (!entry.getSecond().hasItemMeta())
                    continue;
                if (updatesHidden(entry.getSecond()))
                    event.setCancelled(true);
                PaperSlot slot = PaperSlot.slot(plugin, entry::getSecond, entry::setSecond);
                plugin.persistenceManager().load(entry.getSecond()).ifPresent(node -> {
                    if (new PaperEvent.ShowItem(node, user, slot).call())
                        event.setCancelled(true);
                });
            }
        }
    }
}
