package com.github.aecsocket.sokol.core

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
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.serialize.SerializationException
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
            inputQueue.remove()
            registry[input]?.let {
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

class ComponentParser<C : Any, T : NodeComponent>(
    platform: SokolPlatform<T, *, *, *>
) : RegistryItemParser<C, T>(platform.components) {
    override fun exceptionOf(input: String, context: CommandContext<*>) =
        ComponentArgumentException(input, context)

    companion object {
        val ARGUMENT_PARSE_FAILURE_COMPONENT = Caption.of("argument.parse.failure.component")
    }
}

open class ComponentArgument<C : Any, T : NodeComponent>(
    platform: SokolPlatform<T, *, *, *>,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    clazz: Class<T>,
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, T>(required, name, ComponentParser(platform), defaultValue, clazz, suggestionsProvider, description)

class BlueprintParser<C : Any, T : Blueprint<*>>(
    platform: SokolPlatform<*, T, *, *>
) : RegistryItemParser<C, T>(platform.blueprints) {
    override fun exceptionOf(input: String, context: CommandContext<*>) =
        BlueprintArgumentException(input, context)

    companion object {
        val ARGUMENT_PARSE_FAILURE_BLUEPRINT = Caption.of("argument.parse.failure.blueprint")
    }
}

open class BlueprintArgument<C : Any, T : Blueprint<*>>(
    platform: SokolPlatform<*, T, *, *>,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    clazz: Class<T>,
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, T>(required, name, BlueprintParser(platform), defaultValue, clazz, suggestionsProvider, description)


class NodeArgMalformedException(
    context: CommandContext<*>,
    input: String,
    error: Throwable,
) : ParserException(
    NodeParser::class.java, context, NodeParser.ARGUMENT_PARSE_FAILURE_DATA_NODE_MALFORMED,
    CaptionVariable.of("input", input),
    CaptionVariable.of("error", error.message ?: "-"),
)

class NodeArgRegistryException(
    context: CommandContext<*>,
    input: String,
) : ParserException(
    NodeParser::class.java, context, NodeParser.ARGUMENT_PARSE_FAILURE_DATA_NODE_REGISTRY,
    CaptionVariable.of("input", input),
)

class NodeParser<C : Any, O : NodeComponent, T : DataNode>(
    private val platform: SokolPlatform<O, *, *, T>
) : ArgumentParser<C, T> {
    override fun parse(
        commandContext: CommandContext<C>,
        inputQueue: Queue<String>
    ): ArgumentParseResult<T> {
        return inputQueue.peek()?.let { input ->
            inputQueue.remove()
            try {
                ArgumentParseResult.success(platform.persistence.stringToNode(input))
            } catch (ex: SerializationException) {
                ArgumentParseResult.failure(NodeArgMalformedException(
                    commandContext, input, ex
                ))
            } catch (ex: ConfigurateException) {
                platform.components[input]?.let {
                    ArgumentParseResult.success(platform.nodeOf(it))
                } ?: ArgumentParseResult.failure(NodeArgRegistryException(
                    commandContext, input
                ))
            }
        } ?: ArgumentParseResult.failure(NoInputProvidedException(
            NodeParser::class.java,
            commandContext
        ))
    }

    override fun suggestions(commandContext: CommandContext<C>, input: String) =
        platform.components.entries.keys.toMutableList()

    companion object {
        val ARGUMENT_PARSE_FAILURE_DATA_NODE_MALFORMED = Caption.of("argument.parse.failure.data_node.malformed")
        val ARGUMENT_PARSE_FAILURE_DATA_NODE_REGISTRY = Caption.of("argument.parse.failure.data_node.registry")
    }
}

open class NodeArgument<C : Any, T : DataNode>(
    platform: SokolPlatform<*, *, *, T>,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    clazz: Class<T>,
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, T>(required, name, NodeParser(platform), defaultValue, clazz, suggestionsProvider, description)
