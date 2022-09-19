package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.captions.Caption
import cloud.commandframework.context.CommandContext
import com.gitlab.aecsocket.alexandria.core.command.RegistryElementArgument

class EntityBlueprintArgument<C : Any>(
    sokol: Sokol,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : RegistryElementArgument<C, KeyedEntityBlueprint>(
    name, sokol.entityBlueprints, ARGUMENT_PARSE_FAILURE_ENTITY_BLUEPRINT, KeyedEntityBlueprint::class.java,
    description, required, defaultValue, suggestionsProvider
) {
    companion object {
        val ARGUMENT_PARSE_FAILURE_ENTITY_BLUEPRINT = Caption.of("argument.parse.failure.entity_blueprint")
    }
}
