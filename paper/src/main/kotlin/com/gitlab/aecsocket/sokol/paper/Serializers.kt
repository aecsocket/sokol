package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.paper.component.CompositePath
import com.gitlab.aecsocket.sokol.paper.component.compositePathOf
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object CompositePathSerializer : TypeSerializer<CompositePath> {
    override fun serialize(type: Type, obj: CompositePath?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            node.setList(String::class.java, obj)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): CompositePath {
        return compositePathOf(node.force<ArrayList<String>>())
    }
}
