package com.github.aecsocket.sokol.paper.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.captions.Caption;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import com.github.aecsocket.sokol.paper.PaperBlueprintNode;
import com.github.aecsocket.sokol.paper.PaperComponent;
import com.github.aecsocket.sokol.paper.SokolPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * Command argument which parses a {@link PaperComponent}.
 * @param <C> The command sender type.
 */
public final class ComponentArgument<C> extends CommandArgument<C, PaperComponent> {
    /** When a value is not found in the registry. */
    public static final Caption ARGUMENT_PARSE_FAILURE_COMPONENT_REGISTRY = Caption.of("argument.parse.failure.component.registry");

    private ComponentArgument(
        final SokolPlugin plugin,
        final boolean required,
        final @NonNull String name,
        final @NonNull String defaultValue,
        final @Nullable BiFunction<@NonNull CommandContext<C>,
            @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider,
        final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, new ComponentParser<>(plugin), defaultValue, PaperComponent.class, suggestionsProvider, defaultDescription);
    }

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
    public static <C> @NonNull CommandArgument<C, PaperComponent> of(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return ComponentArgument.<C>newBuilder(plugin, name).asRequired().build();
    }

    /**
     * Create a new optional command component
     *
     * @param plugin Plugin
     * @param name   Component name
     * @param <C>    Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, PaperComponent> optional(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return ComponentArgument.<C>newBuilder(plugin, name).asOptional().build();
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
    public static <C> @NonNull CommandArgument<C, PaperComponent> optional(
        final @NonNull SokolPlugin plugin,
        final @NonNull String name,
        final @NonNull PaperComponent defaultValue
    ) {
        return ComponentArgument.<C>newBuilder(plugin, name).asOptionalWithDefault(defaultValue.toString()).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, PaperComponent> {
        private final SokolPlugin plugin;

        private Builder(final @NonNull SokolPlugin plugin, final @NonNull String name) {
            super(PaperComponent.class, name);
            this.plugin = plugin;
        }

        /**
         * Builder a new example component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull ComponentArgument<C> build() {
            return new ComponentArgument<>(
                plugin,
                this.isRequired(),
                this.getName(),
                this.getDefaultValue(),
                this.getSuggestionsProvider(),
                this.getDefaultDescription()
            );
        }

    }

    public static final class ComponentParser<C> implements ArgumentParser<C, PaperComponent> {
        private final SokolPlugin plugin;

        public ComponentParser(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NonNull ArgumentParseResult<PaperComponent> parse(
            final @NonNull CommandContext<C> ctx,
            final @NonNull Queue<@NonNull String> inputQueue
        ) {
            final String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(
                    PaperBlueprintNode.class,
                    ctx
                ));
            }
            inputQueue.remove();

            return plugin.components().get(input)
                .map(ArgumentParseResult::success)
                .orElseGet(() -> ArgumentParseResult.failure(new RegistryException(input, ctx)));
        }

        @Override
        public boolean isContextFree() {
            return false;
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<C> ctx, @NonNull String input) {
            return new ArrayList<>(plugin.components().keySet());
        }
    }

    public static final class RegistryException extends ParserException {
        public RegistryException(String input, CommandContext<?> ctx) {
            super(PaperBlueprintNode.class, ctx, ARGUMENT_PARSE_FAILURE_COMPONENT_REGISTRY,
                CaptionVariable.of("input", input));
        }
    }
}

