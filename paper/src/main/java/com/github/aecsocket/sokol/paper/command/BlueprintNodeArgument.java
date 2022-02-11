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
import com.github.aecsocket.minecommons.core.Text;
import com.github.aecsocket.sokol.paper.PaperBlueprintNode;
import com.github.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * Command argument which parses a {@link PaperBlueprintNode}.
 * @param <C> The command sender type.
 */
public final class BlueprintNodeArgument<C> extends CommandArgument<C, PaperBlueprintNode> {
    /** When a component for the node is not found in the registry. */
    public static final Caption ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_REGISTRY = Caption.of("argument.parse.failure.blueprint_node.registry");
    /** When a node cannot be parsed. */
    public static final Caption ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_GENERIC = Caption.of("argument.parse.failure.blueprint_node.generic");
    /** The token used for referencing the currently held node. */
    public static final String SELF = ".";

    private BlueprintNodeArgument(
        final SokolPlugin plugin,
        final boolean required,
        final @NonNull String name,
        final @NonNull String defaultValue,
        final @Nullable BiFunction<@NonNull CommandContext<C>,
            @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider,
        final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, new BlueprintNodeParser<>(plugin), defaultValue, PaperBlueprintNode.class, suggestionsProvider, defaultDescription);
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
    public static <C> @NonNull CommandArgument<C, PaperBlueprintNode> of(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return BlueprintNodeArgument.<C>newBuilder(plugin, name).asRequired().build();
    }

    /**
     * Create a new optional command component
     *
     * @param plugin Plugin
     * @param name   Component name
     * @param <C>    Command sender type
     * @return Created component
     */
    public static <C> @NonNull CommandArgument<C, PaperBlueprintNode> optional(final @NonNull SokolPlugin plugin, final @NonNull String name) {
        return BlueprintNodeArgument.<C>newBuilder(plugin, name).asOptional().build();
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
    public static <C> @NonNull CommandArgument<C, PaperBlueprintNode> optional(
        final @NonNull SokolPlugin plugin,
        final @NonNull String name,
        final @NonNull PaperBlueprintNode defaultValue
    ) {
        return BlueprintNodeArgument.<C>newBuilder(plugin, name).asOptionalWithDefault(defaultValue.toString()).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, PaperBlueprintNode> {
        private final SokolPlugin plugin;

        private Builder(final @NonNull SokolPlugin plugin, final @NonNull String name) {
            super(PaperBlueprintNode.class, name);
            this.plugin = plugin;
        }

        /**
         * Builder a new example component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull BlueprintNodeArgument<C> build() {
            return new BlueprintNodeArgument<>(
                plugin,
                this.isRequired(),
                this.getName(),
                this.getDefaultValue(),
                this.getSuggestionsProvider(),
                this.getDefaultDescription()
            );
        }

    }

    public static final class BlueprintNodeParser<C> implements ArgumentParser<C, PaperBlueprintNode> {
        private final SokolPlugin plugin;

        public BlueprintNodeParser(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NonNull ArgumentParseResult<PaperBlueprintNode> parse(
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

            PaperBlueprintNode value;
            if (
                SELF.equals(input)
                && ctx.getSender() instanceof Player player
                && (value = plugin.persistence().load(player.getInventory().getItemInMainHand()).orElse(null)) != null
            ) {
                return ArgumentParseResult.success(value);
            }

            try {
                value = plugin.loaderBuilder()
                    .buildAndLoadString(input)
                    .get(PaperBlueprintNode.class);
                return value == null
                    ? ArgumentParseResult.failure(new ParseException(input, ctx, new NullPointerException()))
                    : ArgumentParseResult.success(value);
            } catch (SerializationException e) {
                return ArgumentParseResult.failure(new ParseException(input, ctx, e));
            } catch (ConfigurateException e) {
                return plugin.components().get(input)
                    .map(comp -> ArgumentParseResult.success(new PaperBlueprintNode(comp)))
                    .orElseGet(() -> ArgumentParseResult.failure(new RegistryException(input, ctx)));
            }
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

    public static final class RegistryException extends ParserException {
        public RegistryException(String input, CommandContext<?> ctx) {
            super(PaperBlueprintNode.class, ctx, ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_REGISTRY,
                CaptionVariable.of("input", input));
        }
    }

    public static final class ParseException extends ParserException {
        public ParseException(String input, CommandContext<?> ctx, Exception e) {
            super(PaperBlueprintNode.class, ctx, ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_GENERIC,
                CaptionVariable.of("input", input),
                CaptionVariable.of("error", Text.mergeMessages(e)));
        }
    }
}

