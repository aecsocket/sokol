package old.wrapper;

import com.github.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.Numbers;

import old.SokolPlugin;
import old.impl.PaperNode;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public record PaperItem(SokolPlugin plugin, ItemStack handle) implements Item.Scoped<PaperItem, PaperNode> {
    @Override
    public Optional<PaperNode> node() {
        return plugin.persistence().safeLoad(handle);
    }

    @Override public int amount() {
        return handle.getAmount();
    }

    @Override
    public PaperItem amount(int amount) {
        handle.setAmount(amount);
        return this;
    }

    @Override
    public PaperItem add(int amount) {
        return amount(handle.getAmount() + amount);
    }

    @Override
    public Component name() {
        return handle.getItemMeta().displayName();
    }

    @Override
    public PaperItem name(Component name) {
        handle.editMeta(meta -> meta.displayName(Components.BLANK.append(name)));
        return this;
    }

    @Override
    public List<Component> description() {
        return handle.getItemMeta().lore();
    }

    @Override
    public PaperItem description(List<Component> description) {
        handle.editMeta(meta -> meta.lore(description));
        return this;
    }

    @Override
    public PaperItem addDescription(List<Component> description) {
        if (description.size() == 0)
            return this;
        handle.editMeta(meta -> {
            List<Component> lore = meta.lore();
            if (lore == null || lore.size() == 0)
                meta.lore(description.stream().map(Components.BLANK::append).collect(Collectors.toList()));
            else {
                lore.add(Component.empty());
                lore.addAll(description.stream().map(Components.BLANK::append).collect(Collectors.toList()));
                meta.lore(lore);
            }
        });
        return this;
    }

    @Override
    public OptionalDouble durability() {
        if (handle.getItemMeta() instanceof Damageable meta)
            return OptionalDouble.of(1 - ((double) meta.getDamage() / handle.getType().getMaxDurability()));
        return OptionalDouble.empty();
    }

    @Override
    public PaperItem durability(double percent) {
        handle.editMeta(m -> {
            if (m instanceof Damageable meta) {
                int max = handle.getType().getMaxDurability();
                int damage = (int) Numbers.clamp(max * (1 - percent), 1, max - 1);
                meta.setDamage(damage);
            }
        });
        return this;
    }

    @Override
    public PaperItem maxDurability() {
        handle.editMeta(m -> {
            if (m instanceof Damageable meta)
                meta.setDamage(0);
        });
        return this;
    }
}
