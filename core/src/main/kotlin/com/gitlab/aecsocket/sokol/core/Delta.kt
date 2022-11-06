package com.gitlab.aecsocket.sokol.core

import kotlin.reflect.KProperty

class Delta<T>(var value: T, dirty: Boolean = false) {
    var dirty = dirty
        private set

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        dirty = true
    }

    fun dirty(): Delta<T> {
        dirty = true
        return this
    }

    fun <R> ifDirty(consumer: (T) -> R): R? {
        return if (dirty) consumer(value) else null
    }

    override fun toString() = "($value)${if (dirty) "*" else ""}"
}
