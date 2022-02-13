package com.github.aecsocket.sokol.paper;

import java.util.Map;
import java.util.Set;

import com.github.aecsocket.sokol.core.impl.AbstractComponent;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.rule.RuleTypes;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.github.aecsocket.sokol.core.stat.impl.PrimitiveStat;
import com.github.aecsocket.sokol.core.stat.impl.StringStat;
import com.github.aecsocket.sokol.paper.stat.ItemStat;

public final class PaperComponent extends AbstractComponent<
    PaperComponent, PaperNodeSlot, PaperFeatureProfile<?, ?>
> {
    public static final ItemStat STAT_ITEM = ItemStat.itemStat("item");

    public static final StatTypes STAT_TYPES = StatTypes.builder()
        .add(STAT_ITEM)
        .add(StringStat.stat("string"))
        .add(PrimitiveStat.integer("integer"))
        .build();
    public static final RuleTypes RULE_TYPES = RuleTypes.empty();

    private final SokolPlugin platform;

    PaperComponent(SokolPlugin platform, String id, Set<String> tags, Map<String, PaperFeatureProfile<?, ?>> features, Map<String, ? extends PaperNodeSlot> slots, StatIntermediate stats) {
        super(id, tags, features, slots, stats);
        this.platform = platform;
    }

    @Override public SokolPlugin platform() { return platform; }

    public static final class Serializer extends AbstractComponent.Serializer<
        PaperComponent, PaperNodeSlot, PaperFeature<?>, PaperFeatureProfile<?, ?>
    > {
        private final SokolPlugin platform;

        public Serializer(SokolPlugin platform) {
            this.platform = platform;
        }

        @Override public SokolPlugin platform() { return platform; }

        @Override protected Map<String, Stat<?>> defaultStatTypes() { return STAT_TYPES.map(); }
        @Override protected Map<String, Class<? extends Rule>> defaultRuleTypes() { return RULE_TYPES.map(); }
        @Override protected Class<PaperNodeSlot> slotType() { return PaperNodeSlot.class; }

        @Override
        protected PaperComponent create(
            String id, Set<String> tags, Map<String, PaperFeatureProfile<?, ?>> features, Map<String, PaperNodeSlot> slots, StatIntermediate stats
        ) {
            return new PaperComponent(platform, id, tags, features, slots, stats);
        }
    }
}
