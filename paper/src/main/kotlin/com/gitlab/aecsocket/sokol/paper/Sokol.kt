package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.BasePlugin
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleRepeating
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException

private lateinit var instance: Sokol
val SokolAPI get() = instance

class Sokol : BasePlugin() {
    @ConfigSerializable
    data class Settings(
        val enabled: Boolean = false
    )

    lateinit var settings: Settings private set

    val objectResolver = ObjectResolver(this)

    init {
        instance = this
    }

    override fun onEnable() {
        super.onEnable()
        SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onLoad = {
                addDefaultI18N()
            }
        )

        scheduleRepeating {
            objectResolver.resolve()
        }
    }

    /*
    example nbt

    Player: {
      hygieia:body_state: {
        body_parts: {
          head: ...
          torso: ...
        }
        illnesses: [ ... ]
      }
      norvinsk:more_data: {
        ...
      }
    }

    ItemStack[IRON_NUGGET]: {
      calibre:launcher: {
        id: "m4a1_receiver"
      }
      calibre:sight: {
        id: "m4a1_stock_sight"
      }
      sokol:container: {
        [UUID to "magazine"]: {
          calibre:
        }
      }
    }
     */

    override fun loadInternal(log: LogList, settings: ConfigurationNode): Boolean {
        if (super.loadInternal(log, settings)) {
            try {
                this.settings = settings.get { Settings() }
            } catch (ex: SerializationException) {
                log.line(LogLevel.Error, ex) { "Could not load settings file" }
                return false
            }

            try {
                objectResolver.load(settings)
            } catch (ex: Exception) {
                log.line(LogLevel.Error, ex) { "Could not load entity resolver settings" }
                return false
            }

            return true
        }
        return false
    }
}
