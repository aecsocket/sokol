package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.captions.Caption
import cloud.commandframework.context.CommandContext
import com.gitlab.aecsocket.alexandria.core.command.RegistryElementArgument

class ItemBlueprintArgument<C : Any>(
    sokol: Sokol,
    name: String,
    description: ArgumentDescription = ArgumentDescription.of(""),
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : RegistryElementArgument<C, KeyedItemBlueprint>(
    name, sokol.itemBlueprints, ARGUMENT_PARSE_FAILURE_ITEM_BLUEPRINT, KeyedItemBlueprint::class.java,
    description, required, defaultValue, suggestionsProvider
) {
    companion object {
        val ARGUMENT_PARSE_FAILURE_ITEM_BLUEPRINT = Caption.of("argument.parse.failure.item_blueprint")
    }
}

class EntityBlueprintArgument<C : Any>(
    sokol: Sokol,
    name: String,
    description: ArgumentDescription = ArgumentDescription.of(""),
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : RegistryElementArgument<C, KeyedMobBlueprint>(
    name, sokol.mobBlueprints, ARGUMENT_PARSE_FAILURE_ENTITY_BLUEPRINT, KeyedMobBlueprint::class.java,
    description, required, defaultValue, suggestionsProvider
) {
    companion object {
        val ARGUMENT_PARSE_FAILURE_ENTITY_BLUEPRINT = Caption.of("argument.parse.failure.entity_blueprint")
    }
}
