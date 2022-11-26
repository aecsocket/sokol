package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import kotlin.reflect.KClass

interface EntitySlot : SokolComponent {
    override val componentType get() = EntitySlot::class

    val shape: Shape

    fun allows(): Boolean

    fun attach(child: SokolEntity)
}

object EntitySlotTarget : SokolSystem
