package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.context.CommandContext
import com.gitlab.aecsocket.sokol.core.BlueprintArgument
import com.gitlab.aecsocket.sokol.core.ComponentArgument
import com.gitlab.aecsocket.sokol.core.NodeArgument

class PaperComponentArgument<C : Any>(
    plugin: Sokol,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : com.gitlab.aecsocket.sokol.core.ComponentArgument<C, PaperComponent>(plugin, name, description, required, defaultValue, PaperComponent::class.java, suggestionsProvider)

class PaperBlueprintArgument<C : Any>(
    plugin: Sokol,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : com.gitlab.aecsocket.sokol.core.BlueprintArgument<C, PaperBlueprint>(plugin, name, description, required, defaultValue, PaperBlueprint::class.java, suggestionsProvider)

class PaperNodeArgument<C : Any>(
    plugin: Sokol,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : com.gitlab.aecsocket.sokol.core.NodeArgument<C, PaperDataNode>(plugin, name, description, required, defaultValue, PaperDataNode::class.java, suggestionsProvider)
