package com.gitlab.aecsocket.sokol.core.serializer

import com.gitlab.aecsocket.alexandria.core.serializer.deserializeDefaultedMap
import com.gitlab.aecsocket.alexandria.core.serializer.serializeDefaultedMap
import com.gitlab.aecsocket.sokol.core.util.TableFormat
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val ALIGN = "align"
private const val JUSTIFY = "justify"
private const val COL_SEPARATOR_KEY = "col_separator_key"
private const val ROW_SEPARATOR_KEY = "row_separator_key"

object TableFormatSerializer : TypeSerializer<TableFormat> {
    override fun serialize(type: Type, obj: TableFormat?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            serializeDefaultedMap(obj.align, node.node(ALIGN))
            serializeDefaultedMap(obj.justify, node.node(JUSTIFY))
            node.node(COL_SEPARATOR_KEY).set(obj.colSeparatorKey)
            node.node(ROW_SEPARATOR_KEY).set(obj.rowSeparatorKey)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode) = TableFormat(
        deserializeDefaultedMap(node.node(ALIGN)),
        deserializeDefaultedMap(node.node(JUSTIFY)),
        node.node(COL_SEPARATOR_KEY).get(),
        node.node(ROW_SEPARATOR_KEY).get(),
    )
}
