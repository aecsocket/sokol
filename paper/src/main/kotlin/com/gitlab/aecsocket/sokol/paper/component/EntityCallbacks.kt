package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import net.kyori.adventure.key.Key

fun interface EntityCallback {
    fun run(): Boolean
}

data class EntityCallbacks(
    val callbacks: Map<Key, EntityCallback>
) : SokolComponent {
    override val componentType get() = EntityCallbacks::class

    fun callback(key: Key) = callbacks[key]

    fun call(key: Key) = callbacks[key]?.run()
}

object EntityCallbacksTarget : SokolSystem

@None(EntityCallbacks::class)
@Before(EntityCallbacksTarget::class)
class EntityCallbacksSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mEntityCallbacks = ids.mapper<EntityCallbacks>()

    data class Construct(
        internal val callbacks: MutableMap<Key, EntityCallback> = HashMap()
    ) : SokolEvent {
        fun callback(key: Key, callback: EntityCallback) {
            if (callbacks.contains(key))
                throw IllegalArgumentException("Duplicate callback $key")
            callbacks[key] = callback
        }
    }

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val (callbacks) = entity.callSingle(Construct())
        mEntityCallbacks.set(entity, EntityCallbacks(callbacks))
    }
}
