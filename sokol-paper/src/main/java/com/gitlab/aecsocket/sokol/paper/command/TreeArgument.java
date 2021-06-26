package com.gitlab.aecsocket.sokol.paper.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.captions.Caption;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import com.gitlab.aecsocket.minecommons.core.Text;
import com.gitlab.aecsocket.sokol.paper.PaperComponent;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class TreeArgument<C> extends CommandArgument<C, PaperTreeNode> {
    public static final Caption ARGUMENT_PARSE_FAILURE_TREE = Caption.of("argument.parse.failure.tree");
    public static final Caption ARGUMENT_PARSE_FAILURE_TREE_INVALID = Caption.of("argument.parse.failure.tree.invalid");
    public static final String SELF = ".";

    private final Predicate<PaperTreeNode> test;

    private TreeArgument(
            final SokolPlugin plugin,
            final boolean required,
            final @NonNull String name,
            final @NotNull Predicate<PaperTreeNode> test,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>,
                    @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, new TreeParser<>(plugin, test), defaultValue, PaperTreeNode.class, suggestionsProvider, defaultDescription);
        this.test = test;
    }

    public Predicate<PaperTreeNode> test() { return test; }

    /**
     * Create a new builder
     *
     * @param plugin Plugin
     * @param name   Name of the component
     * @param <C>    Command sender type
     * @return Created builder
     */
    public static <C> @NonNull Builder<C> newBuilder(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return new Builder<>(plugin, name);
    }

    /**
     * Create a new required command component
     *
     * @param plugin Plugin
     * @param name   Component name
     * @param <C>    Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, PaperTreeNode> of(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return TreeArgument.<C>newBuilder(plugin, name).asRequired().build();
    }

    /**
     * Create a new optional command component
     *
     * @param plugin Plugin
     * @param name   Component name
     * @param <C>    Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, PaperTreeNode> optional(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return TreeArgument.<C>newBuilder(plugin, name).asOptional().build();
    }

    /**
     * Create a new required command component with a default value
     *
     * @param plugin       Plugin
     * @param name         Component name
     * @param defaultValue Default value
     * @param <C>          Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, PaperTreeNode> optional(
            final @NonNull SokolPlugin plugin,
            final @NonNull String name,
            final @NonNull PaperComponent defaultValue
    ) {
        return TreeArgument.<C>newBuilder(plugin, name).asOptionalWithDefault(defaultValue.toString()).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, PaperTreeNode> {
        private final SokolPlugin plugin;
        private Predicate<PaperTreeNode> test;

        private Builder(final @NonNull SokolPlugin plugin, final @NonNull String name) {
            super(PaperTreeNode.class, name);
            this.plugin = plugin;
        }

        public Builder<C> test(Predicate<PaperTreeNode> test) { this.test = test; return this; }

        /**
         * Builder a new example component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull TreeArgument<C> build() {
            return new TreeArgument<>(
                    plugin,
                    this.isRequired(),
                    this.getName(),
                    test,
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription()
            );
        }

    }

    public static final class TreeParser<C> implements ArgumentParser<C, PaperTreeNode> {
        private final SokolPlugin plugin;
        private final Predicate<PaperTreeNode> test;

        public TreeParser(SokolPlugin plugin, Predicate<PaperTreeNode> test) {
            this.plugin = plugin;
            this.test = test;
        }

        @Override
        public @NonNull ArgumentParseResult<PaperTreeNode> parse(
                final @NonNull CommandContext<C> ctx,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
            final String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(
                        PaperComponent.class,
                        ctx
                ));
            }
            inputQueue.remove();

            PaperTreeNode value;
            if (SELF.equals(input)
                    && ctx.getSender() instanceof Player player
                    && (value = plugin.persistenceManager().load(player.getInventory().getItemInMainHand()).orElse(null)) != null) {
                return ArgumentParseResult.success(value);
            }

            try {
                value = plugin.loaderBuilder()
                        .buildAndLoadString(input)
                        .get(PaperTreeNode.class);
                if (value == null)
                    return ArgumentParseResult.failure(new ParseException(input, ctx, new NullPointerException()));
            } catch (ConfigurateException e) {
                return ArgumentParseResult.failure(new ParseException(input, ctx, e));
            }

            if (test != null && !test.test(value))
                return ArgumentParseResult.failure(new InvalidException(value.value().id(), ctx));

            return ArgumentParseResult.success(value);
        }

        @Override
        public boolean isContextFree() {
            return false;
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<C> ctx, @NonNull String input) {
            return Collections.singletonList(SELF); // TODO tree suggestions
        }
    }

    public static final class ParseException extends ParserException {
        public ParseException(String input, CommandContext<?> ctx, Exception e) {
            super(PaperComponent.class, ctx, ARGUMENT_PARSE_FAILURE_TREE,
                    CaptionVariable.of("input", input),
                    CaptionVariable.of("exception", Text.mergeMessages(e)));
        }
    }

    public static final class InvalidException extends ParserException {
        public InvalidException(String id, CommandContext<?> ctx) {
            super(PaperComponent.class, ctx, ARGUMENT_PARSE_FAILURE_TREE_INVALID,
                    CaptionVariable.of("id", id));
        }
    }
}
