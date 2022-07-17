package com.gitlab.aecsocket.sokol.core

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.captions.Caption
import cloud.commandframework.captions.CaptionVariable
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import cloud.commandframework.exceptions.parsing.ParserException
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.keyed.Registry
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
) : com.gitlab.aecsocket.sokol.core.RegistryArgumentException(
    com.gitlab.aecsocket.sokol.core.ComponentParser::class.java,
    com.gitlab.aecsocket.sokol.core.ComponentParser.Companion.ARGUMENT_PARSE_FAILURE_COMPONENT,
    input, context
)

class BlueprintArgumentException(
    input: String,
    context: CommandContext<*>
) : com.gitlab.aecsocket.sokol.core.RegistryArgumentException(
    com.gitlab.aecsocket.sokol.core.BlueprintParser::class.java,
    com.gitlab.aecsocket.sokol.core.BlueprintParser.Companion.ARGUMENT_PARSE_FAILURE_BLUEPRINT,
    input, context
)

abstract class RegistryItemParser<C : Any, T : Keyed>(
    private val registry: Registry<T>
) : ArgumentParser<C, T> {
    protected abstract fun exceptionOf(input: String, context: CommandContext<*>): com.gitlab.aecsocket.sokol.core.RegistryArgumentException

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
            com.gitlab.aecsocket.sokol.core.ComponentParser::class.java,
            commandContext
        ))
    }

    override fun suggestions(commandContext: CommandContext<C>, input: String) =
        registry.entries.keys.toMutableList()
}

class ComponentParser<C : Any, T : com.gitlab.aecsocket.sokol.core.NodeComponent>(
    platform: com.gitlab.aecsocket.sokol.core.SokolPlatform<T, *, *, *>
) : com.gitlab.aecsocket.sokol.core.RegistryItemParser<C, T>(platform.components) {
    override fun exceptionOf(input: String, context: CommandContext<*>) =
        com.gitlab.aecsocket.sokol.core.ComponentArgumentException(input, context)

    companion object {
        val ARGUMENT_PARSE_FAILURE_COMPONENT = Caption.of("argument.parse.failure.component")
    }
}

open class ComponentArgument<C : Any, T : com.gitlab.aecsocket.sokol.core.NodeComponent>(
    platform: com.gitlab.aecsocket.sokol.core.SokolPlatform<T, *, *, *>,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    clazz: Class<T>,
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, T>(required, name,
    com.gitlab.aecsocket.sokol.core.ComponentParser(platform), defaultValue, clazz, suggestionsProvider, description)

class BlueprintParser<C : Any, T : com.gitlab.aecsocket.sokol.core.Blueprint<*>>(
    platform: com.gitlab.aecsocket.sokol.core.SokolPlatform<*, T, *, *>
) : com.gitlab.aecsocket.sokol.core.RegistryItemParser<C, T>(platform.blueprints) {
    override fun exceptionOf(input: String, context: CommandContext<*>) =
        com.gitlab.aecsocket.sokol.core.BlueprintArgumentException(input, context)

    companion object {
        val ARGUMENT_PARSE_FAILURE_BLUEPRINT = Caption.of("argument.parse.failure.blueprint")
    }
}

open class BlueprintArgument<C : Any, T : com.gitlab.aecsocket.sokol.core.Blueprint<*>>(
    platform: com.gitlab.aecsocket.sokol.core.SokolPlatform<*, T, *, *>,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    clazz: Class<T>,
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, T>(required, name,
    com.gitlab.aecsocket.sokol.core.BlueprintParser(platform), defaultValue, clazz, suggestionsProvider, description)


class NodeArgMalformedException(
    context: CommandContext<*>,
    input: String,
    error: Throwable,
) : ParserException(
    com.gitlab.aecsocket.sokol.core.NodeParser::class.java, context,
    com.gitlab.aecsocket.sokol.core.NodeParser.Companion.ARGUMENT_PARSE_FAILURE_DATA_NODE_MALFORMED,
    CaptionVariable.of("input", input),
    CaptionVariable.of("error", error.message ?: "-"),
)

class NodeArgRegistryException(
    context: CommandContext<*>,
    input: String,
) : ParserException(
    com.gitlab.aecsocket.sokol.core.NodeParser::class.java, context,
    com.gitlab.aecsocket.sokol.core.NodeParser.Companion.ARGUMENT_PARSE_FAILURE_DATA_NODE_REGISTRY,
    CaptionVariable.of("input", input),
)

class NodeParser<C : Any, O : com.gitlab.aecsocket.sokol.core.NodeComponent, T : com.gitlab.aecsocket.sokol.core.DataNode>(
    private val platform: com.gitlab.aecsocket.sokol.core.SokolPlatform<O, *, *, T>
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
                ArgumentParseResult.failure(
                    com.gitlab.aecsocket.sokol.core.NodeArgMalformedException(
                        commandContext, input, ex
                    )
                )
            } catch (ex: ConfigurateException) {
                platform.components[input]?.let {
                    ArgumentParseResult.success(platform.nodeOf(it))
                } ?: ArgumentParseResult.failure(
                    com.gitlab.aecsocket.sokol.core.NodeArgRegistryException(
                        commandContext, input
                    )
                )
            }
        } ?: ArgumentParseResult.failure(NoInputProvidedException(
            com.gitlab.aecsocket.sokol.core.NodeParser::class.java,
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

open class NodeArgument<C : Any, T : com.gitlab.aecsocket.sokol.core.DataNode>(
    platform: com.gitlab.aecsocket.sokol.core.SokolPlatform<*, *, *, T>,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    clazz: Class<T>,
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, T>(required, name,
    com.gitlab.aecsocket.sokol.core.NodeParser(platform), defaultValue, clazz, suggestionsProvider, description)
