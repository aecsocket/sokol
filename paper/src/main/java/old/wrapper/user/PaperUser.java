package old.wrapper.user;

import com.github.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;

import org.bukkit.Location;

public interface PaperUser extends ItemUser {
    Location location();

    @Override
    default Vector3 position() {
        return PaperUtils.toCommons(location());
    }

    @Override
    default Vector3 direction() {
        return PaperUtils.toCommons(location().getDirection());
    }
}
