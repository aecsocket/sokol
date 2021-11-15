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
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.impl.PaperComponent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurateException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Command argument which parses a {@link PaperNode}.
 * @param <C> The command sender type.
 */
public final class NodeArgument<C> extends CommandArgument<C, PaperNode> {
    /** When a node cannot be parsed. */
    public static final Caption ARGUMENT_PARSE_FAILURE_NODE = Caption.of("argument.parse.failure.node");
    /** When a parsed node is not considered valid. */
    public static final Caption ARGUMENT_PARSE_FAILURE_NODE_INVALID = Caption.of("argument.parse.failure.node.invalid");
    /** The token used for referencing the currently held node. */
    public static final String SELF = ".";

    private final Predicate<PaperNode> test;

    private NodeArgument(
            final SokolPlugin plugin,
            final boolean required,
            final @NonNull String name,
            final Predicate<PaperNode> test,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>,
                    @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, new NodeParser<>(plugin, test), defaultValue, PaperNode.class, suggestionsProvider, defaultDescription);
        this.test = test;
    }

    public Predicate<PaperNode> test() { return test; }

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
    public static <C> @NonNull CommandArgument<C, PaperNode> of(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return NodeArgument.<C>newBuilder(plugin, name).asRequired().build();
    }

    /**
     * Create a new optional command component
     *
     * @param plugin Plugin
     * @param name   Component name
     * @param <C>    Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, PaperNode> optional(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return NodeArgument.<C>newBuilder(plugin, name).asOptional().build();
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
    public static <C> @NonNull CommandArgument<C, PaperNode> optional(
            final @NonNull SokolPlugin plugin,
            final @NonNull String name,
            final @NonNull PaperComponent defaultValue
    ) {
        return NodeArgument.<C>newBuilder(plugin, name).asOptionalWithDefault(defaultValue.toString()).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, PaperNode> {
        private final SokolPlugin plugin;
        private Predicate<PaperNode> test;

        private Builder(final @NonNull SokolPlugin plugin, final @NonNull String name) {
            super(PaperNode.class, name);
            this.plugin = plugin;
        }

        /**
         * Specifies a test that a node tree must pass to be considered valid.
         * @param test The test.
         * @return This instance.
         */
        public Builder<C> test(Predicate<PaperNode> test) { this.test = test; return this; }

        /**
         * Builder a new example component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull NodeArgument<C> build() {
            return new NodeArgument<>(
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

    public static final class NodeParser<C> implements ArgumentParser<C, PaperNode> {
        private final SokolPlugin plugin;
        private final @Nullable Predicate<PaperNode> test;

        public NodeParser(SokolPlugin plugin, @Nullable Predicate<PaperNode> test) {
            this.plugin = plugin;
            this.test = test;
        }

        @Override
        public @NonNull ArgumentParseResult<PaperNode> parse(
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

            PaperNode value;
            if (SELF.equals(input)
                    && ctx.getSender() instanceof Player player
                    && (value = plugin.persistence().safeLoad(player.getInventory().getItemInMainHand()).orElse(null)) != null) {
                return ArgumentParseResult.success(value);
            }

            try {
                value = plugin.loaderBuilder()
                        .buildAndLoadString(input)
                        .get(PaperNode.class);
            } catch (ConfigurateException e) {
                value = plugin.components().get(input)
                        .map(PaperNode::new)
                        .orElse(null);
            }
            if (value == null)
                return ArgumentParseResult.failure(new ParseException(input, ctx, new NullPointerException()));

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
            // TODO tree suggestions
            List<String> result = new ArrayList<>();
            result.add(SELF);
            result.addAll(plugin.components().keySet());
            return result;
        }
    }

    public static final class ParseException extends ParserException {
        public ParseException(String input, CommandContext<?> ctx, Exception e) {
            super(PaperComponent.class, ctx, ARGUMENT_PARSE_FAILURE_NODE,
                    CaptionVariable.of("input", input),
                    CaptionVariable.of("exception", Text.mergeMessages(e)));
        }
    }

    public static final class InvalidException extends ParserException {
        public InvalidException(String id, CommandContext<?> ctx) {
            super(PaperComponent.class, ctx, ARGUMENT_PARSE_FAILURE_NODE_INVALID,
                    CaptionVariable.of("id", id));
        }
    }
}
