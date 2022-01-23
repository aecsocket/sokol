package old;

import old.impl.PaperFeature;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Locale;
import java.util.Map;

import com.github.aecsocket.minecommons.core.translation.Localizer;
import com.github.aecsocket.sokol.core.LoadProvider;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.stat.StatTypes;

public interface FeatureType {
    PaperFeature<?> createFeature(SokolPlugin platform, ConfigurationNode config) throws SerializationException;

    interface Keyed extends FeatureType, com.github.aecsocket.sokol.core.registry.Keyed, LoadProvider {
        static String renderKey(String id) { return "feature." + id + ".name"; }

        @Override
        default Component render(Locale locale, Localizer lc) {
            return lc.safe(locale, renderKey(id()));
        }
    }

    static FeatureType.Keyed of(String id, StatTypes statTypes, Map<String, Class<? extends NodeRule>> ruleTypes, FeatureType factory) {
        return new FeatureTypeImpl(id, statTypes, ruleTypes, factory);
    }
}
