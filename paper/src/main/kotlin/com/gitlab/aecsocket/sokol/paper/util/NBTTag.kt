package com.gitlab.aecsocket.sokol.paper.util

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.component.CompositePath
import com.gitlab.aecsocket.sokol.paper.component.compositePathOf

fun NBTTagContext.makeBlueprint(sokol: Sokol, blueprint: EntityBlueprint) = makeCompound().apply {
    sokol.persistence.writeBlueprint(blueprint, this)
}

fun NBTTagContext.makeEntity(sokol: Sokol, entity: SokolEntity) = makeCompound().apply {
    sokol.persistence.writeEntity(entity, this)
}

fun NBTTagContext.makeCompositePath(path: CompositePath) = makeList().apply {
    path.forEach { add { makeString(it) } }
}

fun NBTTag.asCompositePath() = asList().run { compositePathOf(map { it.asString() }) }
