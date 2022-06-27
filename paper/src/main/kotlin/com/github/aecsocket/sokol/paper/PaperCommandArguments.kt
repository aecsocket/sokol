package com.github.aecsocket.sokol.paper

import cloud.commandframework.ArgumentDescription
import cloud.commandframework.context.CommandContext
import com.github.aecsocket.sokol.core.BlueprintArgument
import com.github.aecsocket.sokol.core.ComponentArgument
import com.github.aecsocket.sokol.core.NodeArgument

class PaperComponentArgument<C : Any>(
    plugin: SokolPlugin,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : ComponentArgument<C, PaperComponent>(plugin, name, description, required, defaultValue, PaperComponent::class.java, suggestionsProvider)

class PaperBlueprintArgument<C : Any>(
    plugin: SokolPlugin,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : BlueprintArgument<C, PaperBlueprint>(plugin, name, description, required, defaultValue, PaperBlueprint::class.java, suggestionsProvider)

class PaperNodeArgument<C : Any>(
    plugin: SokolPlugin,
    name: String,
    description: ArgumentDescription,
    required: Boolean = true,
    defaultValue: String = "",
    suggestionsProvider: ((CommandContext<C>, String) -> List<String>)? = null,
) : NodeArgument<C, PaperDataNode>(plugin, name, description, required, defaultValue, PaperDataNode::class.java, suggestionsProvider)
