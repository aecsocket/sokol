package com.gitlab.aecsocket.sokol.core.util

import kotlin.math.max

// https://github.com/junkdog/artemis-odb/blob/develop/artemis-core/artemis/src/main/java/com/artemis/utils/IntDeque.java

class IntDeque(capacity: Int = 64) {
    private var data: IntArray
    private var beginIndex = 0
    var size = 0
        private set

    init {
        data = IntArray(capacity)
    }

    fun isEmpty() = size == 0

    fun isNotEmpty() = size > 0

    private fun index(index: Int) =
        (beginIndex + index) % data.size

    operator fun contains(element: Int): Boolean {
        repeat(size) {
            if (element == data[index(it)]) return true
        }
        return false
    }

    operator fun get(index: Int) = data[index(index)]

    private fun grow(newCapacity: Int) {
        val newData = IntArray(newCapacity) { 0 }
        repeat(size) { newData[it] = get(it) }
        data = newData
        beginIndex = 0
    }

    fun add(element: Int) {
        if (size == data.size)
            grow((data.size * 7) / 4 + 1)
        data[index(size++)] = element
    }

    fun set(index: Int, element: Int) {
        if (index >= data.size)
            grow((index * 7) / 4 + 1)
        size = max(size, index + 1)
        data[index(index)] = element
    }

    fun popLast(): Int {
        if (isEmpty()) throw NoSuchElementException()
        return data[index(--size)]
    }

    fun popFirst(): Int {
        if (isEmpty()) throw NoSuchElementException()
        val value = data[beginIndex]
        beginIndex = (beginIndex + 1) % data.size
        size--
        return value
    }

    fun clear() {
        data.fill(0)
        size = 0
        beginIndex = 0
    }
}