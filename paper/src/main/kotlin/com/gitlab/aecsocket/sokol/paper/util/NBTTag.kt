package com.gitlab.aecsocket.sokol.paper.util

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol

fun NBTTagContext.makeBlueprint(sokol: Sokol, blueprint: EntityBlueprint) = makeCompound().apply {
    sokol.persistence.writeBlueprint(blueprint, this)
}

fun NBTTagContext.makeEntity(sokol: Sokol, entity: SokolEntity) = makeCompound().apply {
    sokol.persistence.writeEntity(entity, this)
}
