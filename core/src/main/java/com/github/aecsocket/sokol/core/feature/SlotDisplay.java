package com.github.aecsocket.sokol.core.feature;

import com.github.aecsocket.minecommons.core.Colls;
import com.github.aecsocket.minecommons.core.Components;
import com.github.aecsocket.minecommons.core.Range;
import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.impl.AbstractFeatureInstance;
import com.github.aecsocket.sokol.core.rule.RuleTypes;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.github.aecsocket.sokol.core.world.ItemStack;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.kyori.adventure.text.Component.*;

public abstract class SlotDisplay<
    F extends SlotDisplay<F, P, D, I, N, S>,
    P extends SlotDisplay<F, P, D, I, N, S>.Profile,
    D extends SlotDisplay<F, P, D, I, N, S>.Profile.Data,
    I extends SlotDisplay<F, P, D, I, N, S>.Profile.Instance,
    N extends TreeNode.Scoped<N, ?, ?, ?, S>,
    S extends ItemStack.Scoped<S, ?>
> implements Feature<P> {
    public static final String
        ID = "slot_display",
        KEY_INDENT = "feature." + ID + ".indent",
        KEY_ENTRY = "feature." + ID + ".entry",
        KEY_CHILD = "feature." + ID + ".child",
        KEY_EMPTY = "feature." + ID + ".empty",
        KEY_REQUIRED = "feature." + ID + ".required";

    public enum DisplayOrder {
        DEEPEST_FIRST,
        BROADEST_FIRST
    }

    protected final I18N i18n;

    public SlotDisplay(I18N i18n) {
        this.i18n = i18n;
    }

    protected abstract F self();
    protected abstract SokolPlatform platform();

    @Override public StatTypes statTypes() { return StatTypes.empty(); }
    @Override public RuleTypes ruleTypes() { return RuleTypes.empty(); }
    @Override public final String id() { return ID; }

    public abstract class Profile implements FeatureProfile<F, D> {
        protected final int listenerPriority;
        protected final DisplayOrder displayOrder;
        protected final List<String> forceStart;
        protected final List<String> forceEnd;
        protected final String padding;
        protected final int paddingWidth;

        protected final Set<String> forced;

        public Profile(int listenerPriority, DisplayOrder displayOrder, List<String> forceStart, List<String> forceEnd, String padding, int paddingWidth) {
            this.listenerPriority = listenerPriority;
            this.displayOrder = displayOrder;
            this.forceStart = forceStart;
            this.forceEnd = forceEnd;
            this.padding = padding;
            this.paddingWidth = paddingWidth;
            forced = Colls.joinSet(forceStart, forceEnd);
        }

        public int listenerPriority() { return listenerPriority; }
        public DisplayOrder displayOrder() { return displayOrder; }
        public List<String> forceStart() { return forceStart; }
        public List<String> forceEnd() { return forceEnd; }
        public String padding() { return padding; }
        public int paddingWidth() { return paddingWidth; }

        protected abstract P self();
        @Override public F type() { return SlotDisplay.this.self(); }

        @Override public void validate(SokolComponent parent) throws FeatureValidationException {}

        protected abstract int width(String text);

        public abstract class Data implements FeatureData<P, I, N> {
            @Override public P profile() { return self(); }

            @Override public void save(ConfigurationNode node) throws SerializationException {}
        }

        public abstract class Instance extends AbstractFeatureInstance<P, D, N> {
            @Override public P profile() { return self(); }

            protected abstract void save(ItemState state);

            @Override
            public void build(Tree<N> tree, N parent, StatIntermediate stats) {
                super.build(tree, parent, stats);
                if (parent.isRoot()) {
                    tree.events().register(new TypeToken<NodeEvent.CreateItem<N, ?, S>>() {}, this::onEvent, listenerPriority);

                    tree.itemTransforms().register((stack, state) -> {
                        Range.Integer loreLines = Range.ofInteger(5, 17);
                        loreLines = state.lore(loreLines, Arrays.asList(Component.text("Line 1"), Component.text("Line 2")));
                        save(state);
                    }, itemTransformPriority, ItemTransforms.CREATION, ItemTransforms.TREE);
                }
            }

            private record KeyData(Component component, int width) {}

            protected void onEvent(NodeEvent.CreateItem<N, ?, S> event) {
                N node = event.node();
                S item = event.item();
                Locale locale = node.context().locale();
                List<Component> lines = new ArrayList<>();

                Map<String, KeyData> keyData = new HashMap<>();
                Map<NodePath, Integer> slotDepths = new HashMap<>();
                AtomicInteger longest = new AtomicInteger();
                preLines(keyData, slotDepths, locale, event.node(), longest);
                lines(lines, keyData, slotDepths, locale, event.node(), longest.get(), i18n.line(locale, KEY_INDENT), 0);

                item.addLore(locale, lines);
            }

            protected int preLines(Map<String, KeyData> keyData, Map<NodePath, Integer> slotDepths, Locale locale, N node, AtomicInteger longest) {
                int size = 0;
                for (var entry : node.value().slots().entrySet()) {
                    String key = entry.getKey();
                    size += 1 + node.get(key)
                        .map(child -> preLines(keyData, slotDepths, locale, child, longest))
                        .orElse(0);
                    if (keyData.containsKey(key))
                        continue;
                    Component rendered = entry.getValue().render(i18n, locale);
                    int width = width(PlainTextComponentSerializer.plainText().serialize(rendered));
                    keyData.put(key, new KeyData(rendered, width));
                    if (width > longest.get())
                        longest.set(width);
                }
                slotDepths.put(node.path(), size);
                return size;
            }

            protected void lines(List<Component> lines, Map<String, KeyData> keyData, Map<NodePath, Integer> slotDepths, Locale locale, N node, int longest, Component indent, int depth) {
                Map<String, ? extends NodeSlot> slots = node.value().slots();
                List<String> order = new ArrayList<>();
                for (var slot : slots.keySet()) {
                    if (!forced.contains(slot)) {
                        order.add(slot);
                    }
                }
                order.sort(Comparator.comparingInt(key ->
                    slotDepths.getOrDefault(node.path().append(key), 0)
                        * (displayOrder == DisplayOrder.DEEPEST_FIRST ? -1 : 1)));

                order.addAll(0, forceStart);
                order.addAll(forceEnd);

                for (var key : order) {
                    NodeSlot slot = slots.get(key);
                    if (slot == null)
                        continue;
                    Optional<N> oChild = node.get(key);
                    lines.addAll(i18n.lines(locale, KEY_ENTRY,
                        c -> c.of("padding", () -> text(padding.repeat((longest - keyData.get(key).width) / paddingWidth))),
                        c -> c.of("indent", () -> Components.repeat(indent, depth)),
                        c -> c.of("key", () -> text(key)),
                        c -> c.of("slot", () -> c.rd(slot)),
                        c -> c.of("value", () -> oChild
                            .map(child -> i18n.line(locale, KEY_CHILD,
                                d -> d.of("id", () -> text(child.value().id())),
                                d -> d.of("name", () -> d.rd(child.value()))))
                            .orElseGet(() -> slot.required()
                                ? i18n.line(locale, KEY_REQUIRED)
                                : i18n.line(locale, KEY_EMPTY)))));
                    oChild.ifPresent(child -> lines(lines, keyData, slotDepths, locale, child, longest, indent, depth + 1));
                }
            }
        }
    }
}
