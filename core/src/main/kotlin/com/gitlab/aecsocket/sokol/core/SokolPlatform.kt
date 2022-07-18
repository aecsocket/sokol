package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.keyed.MutableRegistry
import com.gitlab.aecsocket.alexandria.core.keyed.Registry
import com.gitlab.aecsocket.alexandria.core.walkPathed
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.loader.AbstractConfigurationLoader
import org.spongepowered.configurate.serialize.SerializationException
import java.io.File

interface SokolPlatform<
    C : NodeComponent,
    B : Blueprint<*>,
    F : Feature<*>,
    N : DataNode
> {
    val components: Registry<C>
    val blueprints: Registry<B>
    val features: Registry<F>
    val persistence: SokolPersistence<N>

    fun nodeOf(component: C): N

    companion object {
        private const val CONFIG_EXTENSION = "conf"
        private const val IGNORE = "__"
        private const val ENTRIES = "entries"

        fun <T : Keyed> loadRegistry(
            log: LogList,
            loaderBuilder: () -> AbstractConfigurationLoader.Builder<*, *>,
            registry: MutableRegistry<T>,
            root: File,
            type: Class<T>
        ) {
            registry.clear()
            root.walkPathed { file, name, subPath ->
                if (file.isHidden || name.startsWith(IGNORE)) false else {
                    if (name.endsWith(CONFIG_EXTENSION)) {
                        try {
                            loaderBuilder().file(file).build().load()
                        } catch (ex: ConfigurateException) {
                            log.line(LogLevel.Warning, ex) { "Could not parse ${type.simpleName} from ${subPath.joinToString("/")}" }
                            null
                        }?.let { node ->
                            node.node(ENTRIES).childrenMap().forEach { (_, child) ->
                                try {
                                    child.get(type) ?: throw SerializationException(child, type, "Null created (is the deserializer registered?)")
                                } catch (ex: SerializationException) {
                                    log.line(LogLevel.Warning, ex) { "Could not parse ${type.simpleName} from ${subPath.joinToString("/")}" }
                                    null
                                }?.let {
                                    registry.register(it)
                                }
                            }
                        }
                    }
                    true
                }
            }
            log.line(LogLevel.Info) { "Registered ${registry.size}x ${type.simpleName}" }
        }
    }
}
