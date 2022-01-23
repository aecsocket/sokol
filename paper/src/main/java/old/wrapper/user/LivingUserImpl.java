package old.wrapper.user;

import old.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/* package */ class LivingUserImpl<E extends LivingEntity> extends EntityUserImpl<E> implements LivingUser {
    LivingUserImpl(SokolPlugin plugin, E entity) {
        super(plugin, entity);
    }

    @Override public Location location() { return entity.getEyeLocation(); }
}
