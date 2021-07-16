package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.core.wrapper.AudienceUser;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Locale;

public interface EntityUser extends PaperUser, AudienceUser, ForwardingAudience {
    Entity handle();

    @Override default Location location() { return handle().getLocation(); }
    @Override default Locale locale() { return platform().defaultLocale(); }

    @Override default @NotNull Iterable<? extends Audience> audiences() {
        return Collections.singleton(handle());
    }
}
