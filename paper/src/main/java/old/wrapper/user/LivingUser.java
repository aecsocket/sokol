package old.wrapper.user;

import old.SokolPlugin;
import org.bukkit.entity.LivingEntity;

public interface LivingUser extends EntityUser {
    @Override LivingEntity entity();

    static LivingUser user(SokolPlugin plugin, LivingEntity entity) {
        return new LivingUserImpl<>(plugin, entity);
    }
}
