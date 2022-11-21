package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.captions.Caption
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import com.gitlab.aecsocket.alexandria.core.command.ConfigurationNodeParser
import com.gitlab.aecsocket.alexandria.core.command.RegistryElementArgument
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.EntityBlueprint
import com.gitlab.aecsocket.sokol.core.KeyedEntityProfile
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.serialize.SerializationException
import java.util.*

class KeyedEntityProfileArgument<C : Any>(
    sokol: Sokol,
    name: String,
    description: ArgumentDescription = ArgumentDescription.of(""),
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : RegistryElementArgument<C, KeyedEntityProfile>(name, sokol.entityProfiles, ARGUMENT_PARSE_FAILURE_ENTITY_PROFILE, KeyedEntityProfile::class.java, description, required, defaultValue, suggestionsProvider) {
    companion object {
        val ARGUMENT_PARSE_FAILURE_ENTITY_PROFILE = Caption.of("argument.parse.failure.entity_profile")
    }
}

class EntityBlueprintParser<C : Any>(
    private val sokol: Sokol
) : ArgumentParser<C, EntityBlueprint> {
    override fun parse(
        commandContext: CommandContext<C>,
        inputQueue: Queue<String>
    ): ArgumentParseResult<EntityBlueprint> {
        return inputQueue.peek()?.let {
            val input = inputQueue.joinToString(" ")
            inputQueue.clear()

            try {
                val node = AlexandriaAPI.configLoader().buildAndLoadString(input)

                try {
                    ArgumentParseResult.success(node.force<EntityBlueprint>())
                } catch (ex: Exception) {
                    ArgumentParseResult.failure(ex)
                }
            } catch (ex: ConfigurateException) {
                sokol.entityProfile(input)?.let { profile ->
                    ArgumentParseResult.success(sokol.persistence.emptyBlueprint(profile))
                } ?: ArgumentParseResult.failure(ex)
            }
        } ?: ArgumentParseResult.failure(NoInputProvidedException(
            ConfigurationNodeParser::class.java,
            commandContext
        ))
    }

    override fun suggestions(commandContext: CommandContext<C>, input: String): List<String> {
        return sokol.entityProfiles.entries.keys.toList()
    }
}

class EntityBlueprintArgument<C : Any>(
    sokol: Sokol,
    name: String,
    description: ArgumentDescription = ArgumentDescription.of(""),
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : CommandArgument<C, EntityBlueprint>(required, name, EntityBlueprintParser(sokol), defaultValue, EntityBlueprint::class.java, suggestionsProvider, description)
