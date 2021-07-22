package com.gitlab.aecsocket.sokol.paper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.EntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SokolPacketListener extends PacketAdapter {
    private final SokolPlugin plugin;

    public SokolPacketListener(SokolPlugin plugin) {
        super(plugin, PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.ENTITY_EQUIPMENT);
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketType type = event.getPacketType();
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();

        if (type == PacketType.Play.Server.SET_SLOT) {
            ItemStack item = packet.getItemModifier().read(0);
            if (item.hasItemMeta() && plugin.persistenceManager().updatesHidden(item)) {
                event.setCancelled(true);
            }
        }
        if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            EntityUser user = PaperUser.anyEntity(plugin, packet.getEntityModifier(event).read(0));
            for (var entry : packet.getSlotStackPairLists().read(0)) {
                if (!entry.getSecond().hasItemMeta())
                    continue;
                if (plugin.persistenceManager().updatesHidden(entry.getSecond()))
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
