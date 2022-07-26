package com.gitlab.aecsocket.sokol.core.util

import com.gitlab.aecsocket.alexandria.core.*
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TableFormat(
    val align: DefaultedMap<Int, TableAlign> = emptyDefaultedMap(TableAlign.START),
    val justify: DefaultedMap<Int, TableAlign> = emptyDefaultedMap(TableAlign.START),
    val colSeparatorKey: String? = null,
    val rowSeparatorKey: String? = null,
)
