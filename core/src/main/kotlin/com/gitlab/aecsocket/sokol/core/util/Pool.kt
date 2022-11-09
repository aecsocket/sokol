package com.gitlab.aecsocket.sokol.core.util

import kotlin.math.max

// https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils/Pool.java

interface Poolable {
    fun reset()
}

interface Pool<T : Poolable> {
    fun obtain(): T

    fun free(obj: T)

    fun countFree(): Int

    fun peakFree(): Int
}

private class PoolImpl<T : Poolable>(
    val capacity: Int,
    private val createNew: () -> T,
) : Pool<T> {
    private val free = ArrayDeque<T>(capacity)
    private var peak = 0

    override fun obtain() = if (free.isEmpty()) createNew() else free.removeFirst()

    override fun free(obj: T) {
        obj.reset()
        if (free.size < capacity) {
            free.add(obj)
            peak = max(peak, free.size)
        }
    }

    override fun countFree() = free.size

    override fun peakFree() = peak
}

fun <T : Poolable> poolOf(capacity: Int, createNew: () -> T): Pool<T> = PoolImpl(capacity, createNew)
