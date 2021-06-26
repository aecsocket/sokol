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
import com.gitlab.aecsocket.sokol.core.component.Blueprint;
import com.gitlab.aecsocket.sokol.paper.PaperBlueprint;
import com.gitlab.aecsocket.sokol.paper.PaperComponent;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Command argument which parses a {@link Blueprint}.
 * @param <C> The command sender type.
 */
public final class BlueprintArgument<C> extends CommandArgument<C, PaperBlueprint> {
    /** When a blueprint ID cannot be parsed. */
    public static final Caption ARGUMENT_PARSE_FAILURE_BLUEPRINT = Caption.of("argument.parse.failure.blueprint");
    /** When a parsed blueprint is not considered valid. */
    public static final Caption ARGUMENT_PARSE_FAILURE_BLUEPRINT_INVALID = Caption.of("argument.parse.failure.blueprint.invalid");

    private final Predicate<PaperBlueprint> test;

    private BlueprintArgument(
            final SokolPlugin plugin,
            final boolean required,
            final @NonNull String name,
            final @NotNull Predicate<PaperBlueprint> test,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>,
                    @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, new ComponentParser<>(plugin, test), defaultValue, PaperBlueprint.class, suggestionsProvider, defaultDescription);
        this.test = test;
    }

    public Predicate<PaperBlueprint> test() { return test; }

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
    public static <C> @NonNull CommandArgument<C, PaperBlueprint> of(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return BlueprintArgument.<C>newBuilder(plugin, name).asRequired().build();
    }

    /**
     * Create a new optional command component
     *
     * @param plugin Plugin
     * @param name   Component name
     * @param <C>    Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, PaperBlueprint> optional(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return BlueprintArgument.<C>newBuilder(plugin, name).asOptional().build();
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
    public static <C> @NonNull CommandArgument<C, PaperBlueprint> optional(
            final @NonNull SokolPlugin plugin,
            final @NonNull String name,
            final @NonNull PaperComponent defaultValue
    ) {
        return BlueprintArgument.<C>newBuilder(plugin, name).asOptionalWithDefault(defaultValue.toString()).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, PaperBlueprint> {
        private final SokolPlugin plugin;
        private Predicate<PaperBlueprint> test;

        private Builder(final @NonNull SokolPlugin plugin, final @NonNull String name) {
            super(PaperBlueprint.class, name);
            this.plugin = plugin;
        }

        /**
         * Specifies a test that a component must pass to be considered valid.
         * @param test The test.
         * @return This instance.
         */
        public Builder<C> test(Predicate<PaperBlueprint> test) { this.test = test; return this; }

        /**
         * Builder a new example component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull BlueprintArgument<C> build() {
            return new BlueprintArgument<>(
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

    public static final class ComponentParser<C> implements ArgumentParser<C, PaperBlueprint> {
        private final SokolPlugin plugin;
        private final Predicate<PaperBlueprint> test;

        public ComponentParser(SokolPlugin plugin, Predicate<PaperBlueprint> test) {
            this.plugin = plugin;
            this.test = test;
        }

        @Override
        public @NonNull ArgumentParseResult<PaperBlueprint> parse(
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

            return plugin.blueprints().of(input)
                    .<ArgumentParseResult<PaperBlueprint>>map(value -> {
                        if (test != null && !test.test(value))
                            return ArgumentParseResult.failure(new InvalidException(input, ctx));
                        return ArgumentParseResult.success(value);
                    })
                    .orElse(ArgumentParseResult.failure(new ParseException(input, ctx)));
        }

        @Override
        public boolean isContextFree() {
            return true;
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<C> ctx, @NonNull String input) {
            List<String> result = new ArrayList<>();
            for (var entry : plugin.blueprints().entrySet()) {
                if (test == null || test.test(entry.getValue()))
                    result.add(entry.getKey());
            }
            return result;
        }
    }

    public static final class ParseException extends ParserException {
        public ParseException(String input, CommandContext<?> ctx) {
            super(PaperComponent.class, ctx, ARGUMENT_PARSE_FAILURE_BLUEPRINT,
                    CaptionVariable.of("input", input));
        }
    }

    public static final class InvalidException extends ParserException {
        public InvalidException(String input, CommandContext<?> ctx) {
            super(PaperComponent.class, ctx, ARGUMENT_PARSE_FAILURE_BLUEPRINT_INVALID,
                    CaptionVariable.of("input", input));
        }
    }
}
