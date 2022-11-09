package com.gitlab.aecsocket.sokol.core.util

// https://github.com/junkdog/artemis-odb/blob/develop/artemis-core/artemis/src/main/java/com/artemis/utils/IntDeque.java

class IntDeque(capacity: Int = 64) : Iterable<Int> {
    private var elements = intArrayOf(capacity)
    private var start = 0
    var size: Int = 0
        private set

    private fun index(relative: Int) = (start + relative) % elements.size

    fun isEmpty() = size == 0

    operator fun get(index: Int) = elements[index(index)]

    private fun grow(newCapacity: Int) {
        val newElements = intArrayOf(newCapacity)
        repeat(size) {
            newElements[it] = get(it)
        }
        elements = newElements
        start = 0
    }

    private fun ensureCapacity(index: Int) {
        if (index >= elements.size) {
            grow(index)
        }
    }

    fun add(value: Int) {
        if (size == elements.size)
            grow((elements.size * 7) / 4 + 1)
        elements[index(size++)] = value
    }

    operator fun set(index: Int, value: Int) {
        if (index >= elements.size)
            grow((index * 7) / 4 + 1)
        size = kotlin.math.max(size, index + 1)
        elements[index(index)] = value
    }

    fun popLast(): Int {
        if (isEmpty()) throw NoSuchElementException()
        return elements[index(--size)]
    }

    fun popFirst(): Int {
        if (isEmpty()) throw NoSuchElementException()
        val value = elements[start]
        start = (start + 1) % elements.size
        size--
        return value
    }

    fun clear() {
        elements.fill(0)
        size = 0
        start = 0
    }

    override fun iterator() = object : Iterator<Int> {
        var cursor = 0

        override fun hasNext() = cursor < size

        override fun next(): Int {
            if (cursor == size) throw NoSuchElementException()
            return get(cursor++)
        }
    }
}
