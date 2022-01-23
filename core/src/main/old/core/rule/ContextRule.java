package com.gitlab.aecsocket.sokol.core.rule;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Tree;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Locale;
import java.util.Objects;

import static com.gitlab.aecsocket.sokol.core.rule.Rule.wrapBrackets;
import static net.kyori.adventure.text.Component.text;

public final class ContextRule {
    private ContextRule() {}

    public static abstract class AsInline implements Rule {
        protected final Rule term;

        public AsInline(Rule term) {
            this.term = term;
        }

        protected AsInline() {
            term = Constant.TRUE;
        }

        public Rule term() { return term; }

        protected abstract void appliesInline(CompatibilityTree<?> compat) throws RuleException;

        @Override
        public void applies(Node node, Tree<?> tree) throws RuleException {
            if (!(tree instanceof CompatibilityTree<?> compat))
                throw new IllegalStateException("Tree context is of type " + tree.getClass() + ", expected " + CompatibilityTree.class);
            try {
                appliesInline(compat);
            } catch (RuleException e) {
                throw new RuleException(this, e);
            }
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.visit(this);
            term.visit(visitor);
        }

        @Override
        public String toString() { return name(); }

        @Override
        public Component render(Locale locale, Localizer lc) {
            return text(name(), OPERATOR)
                    .append(wrapBrackets(term.render(locale, lc)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AsInline asInline = (AsInline) o;
            return term.equals(asInline.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(term);
        }
    }

    @ConfigSerializable
    public static final class AsParent extends AsInline {
        public static final String TYPE = "as_parent";

        public AsParent(Rule term) {
            super(term);
        }

        private AsParent() {}

        @Override
        public String name() { return TYPE; }

        @Override
        protected void appliesInline(CompatibilityTree<?> compat) throws RuleException {
            term.applies(compat.parent().root(), compat.parent());
        }
    }

    @ConfigSerializable
    public static final class AsChild extends AsInline {
        public static final String TYPE = "as_child";

        public AsChild(Rule term) {
            super(term);
        }

        private AsChild() {}

        @Override
        public String name() { return TYPE; }

        @Override
        protected void appliesInline(CompatibilityTree<?> compat) throws RuleException {
            term.applies(compat.child().root(), compat.child());
        }
    }
}
