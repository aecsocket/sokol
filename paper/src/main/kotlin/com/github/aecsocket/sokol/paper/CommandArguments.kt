package com.github.aecsocket.sokol.paper

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.captions.Caption
import cloud.commandframework.captions.CaptionVariable
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import cloud.commandframework.exceptions.parsing.ParserException
import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.alexandria.core.keyed.Registry
import java.util.*

open class RegistryArgumentException(
    parserType: Class<*>,
    caption: Caption,
    input: String,
    context: CommandContext<*>
) : ParserException(
    parserType, context, caption,
    CaptionVariable.of("input", input)
)

class ComponentArgumentException(
    input: String,
    context: CommandContext<*>
) : RegistryArgumentException(
    ComponentParser::class.java,
    ComponentParser.ARGUMENT_PARSE_FAILURE_COMPONENT,
    input, context
)

class BlueprintArgumentException(
    input: String,
    context: CommandContext<*>
) : RegistryArgumentException(
    BlueprintParser::class.java,
    BlueprintParser.ARGUMENT_PARSE_FAILURE_BLUEPRINT,
    input, context
)

abstract class RegistryItemParser<C : Any, T : Keyed>(
    private val registry: Registry<T>
) : ArgumentParser<C, T> {
    protected abstract fun exceptionOf(input: String, context: CommandContext<*>): RegistryArgumentException

    override fun parse(
        commandContext: CommandContext<C>,
        inputQueue: Queue<String>
    ): ArgumentParseResult<T> {
        return inputQueue.peek()?.let { input ->
            registry[input]?.let {
                inputQueue.remove()
                ArgumentParseResult.success(it)
            } ?: ArgumentParseResult.failure(exceptionOf(input, commandContext))
        } ?: ArgumentParseResult.failure(NoInputProvidedException(
            ComponentParser::class.java,
            commandContext
        ))
    }

    override fun suggestions(commandContext: CommandContext<C>, input: String) =
        registry.entries.keys.toMutableList()
}

class ComponentParser<C : Any>(
    plugin: SokolPlugin
) : RegistryItemParser<C, PaperComponent>(plugin.components) {
    override fun exceptionOf(input: String, context: CommandContext<*>) =
        ComponentArgumentException(input, context)

    companion object {
        val ARGUMENT_PARSE_FAILURE_COMPONENT = Caption.of("argument.parse.failure.component")
    }
}

class ComponentArgument<C : Any>(
    plugin: SokolPlugin,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, PaperComponent>(required, name, ComponentParser(plugin), defaultValue, PaperComponent::class.java, suggestionsProvider, description)

class BlueprintParser<C : Any>(
    plugin: SokolPlugin
) : RegistryItemParser<C, PaperBlueprint>(plugin.blueprints) {
    override fun exceptionOf(input: String, context: CommandContext<*>) =
        BlueprintArgumentException(input, context)

    companion object {
        val ARGUMENT_PARSE_FAILURE_BLUEPRINT = Caption.of("argument.parse.failure.blueprint")
    }
}

class BlueprintArgument<C : Any>(
    plugin: SokolPlugin,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, PaperBlueprint>(required, name, BlueprintParser(plugin), defaultValue, PaperBlueprint::class.java, suggestionsProvider, description)
