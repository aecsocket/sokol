package com.github.aecsocket.sokol.core.feature;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.impl.AbstractFeatureInstance;
import com.github.aecsocket.sokol.core.rule.RuleTypes;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.github.aecsocket.sokol.core.stat.impl.StringStat;
import com.github.aecsocket.sokol.core.item.ItemStack;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class ItemDescription<
    F extends ItemDescription<F, P, D, I, N, S>,
    P extends ItemDescription<F, P, D, I, N, S>.Profile,
    D extends ItemDescription<F, P, D, I, N, S>.Profile.Data,
    I extends ItemDescription<F, P, D, I, N, S>.Profile.Instance,
    N extends TreeNode.Scoped<N, ?, ?, ?, S>,
    S extends ItemStack.Scoped<S, ?>
> implements Feature<P> {
    public static final StringStat STAT_ITEM_NAME_KEY = StringStat.stat("item_name_key");
    public static final StringStat STAT_ITEM_DESCRIPTION_KEY = StringStat.stat("item_description_key");
    public static final StatTypes STATS = StatTypes.builder()
        .add(STAT_ITEM_NAME_KEY)
        .add(STAT_ITEM_DESCRIPTION_KEY)
        .build();
    public static final String
        ID = "item_description",
        KEY_LINE = "feature." + ID + ".line";

    protected final I18N i18n;

    public ItemDescription(I18N i18n) {
        this.i18n = i18n;
    }

    protected abstract F self();
    protected abstract SokolPlatform platform();

    @Override public StatTypes statTypes() { return STATS; }
    @Override public RuleTypes ruleTypes() { return RuleTypes.empty(); }
    @Override public final String id() { return ID; }

    public abstract class Profile implements FeatureProfile<F, D> {
        protected final int listenerPriority;

        public Profile(int listenerPriority) {
            this.listenerPriority = listenerPriority;
        }

        public int listenerPriority() { return listenerPriority; }

        protected abstract P self();
        @Override public F type() { return ItemDescription.this.self(); }

        @Override public void validate(SokolComponent parent) throws FeatureValidationException {}

        public abstract class Data implements FeatureData<P, I, N> {
            @Override public P profile() { return self(); }

            @Override public void save(ConfigurationNode node) throws SerializationException {}
        }

        public abstract class Instance extends AbstractFeatureInstance<P, D, N> {
            @Override public P profile() { return self(); }

            @Override
            public void build(Tree<N> tree, N parent, StatIntermediate stats) {
                super.build(tree, parent, stats);
                if (parent.isRoot()) {
                    tree.events().register(new TypeToken<NodeEvent.CreateItem<N, ?, S>>() {}, this::onEvent, listenerPriority);
                }
            }

            protected void onEvent(NodeEvent.CreateItem<N, ?, S> event) {
                N node = event.node();
                S item = event.item();
                Locale locale = node.context().locale();

                node.tree().stats().value(STAT_ITEM_NAME_KEY).ifPresent(itemNameKey -> {
                    item.name(i18n.line(locale, itemNameKey,
                        c -> c.of("original", item::name)));
                });

                node.tree().stats().value(STAT_ITEM_DESCRIPTION_KEY)
                    .map(itemDescKey -> i18n.lines(locale, itemDescKey))
                    .or(() -> node.value().renderDescription(i18n, locale))
                    .ifPresent(desc -> {
                        List<Component> lines = new ArrayList<>();
                        for (var line : desc) {
                            lines.addAll(i18n.lines(locale, KEY_LINE,
                                c -> c.of("line", () -> line)));
                        }
                        item.addLore(locale, lines);
                    });
            }
        }
    }
}
