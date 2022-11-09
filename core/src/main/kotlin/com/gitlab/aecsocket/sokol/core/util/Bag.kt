package com.gitlab.aecsocket.sokol.core.util

import kotlin.math.max

// adapted from https://github.com/junkdog/artemis-odb/blob/develop/artemis-core/artemis/src/main/java/com/artemis/utils/Bag.java

interface Bag<E> : Collection<E> {
    operator fun get(index: Int): E?
}

interface MutableBag<E> : Bag<E>, MutableCollection<E> {
    operator fun set(index: Int, element: E)

    fun removeAt(index: Int): E
}

private class BagImpl<E>(
    private var data: Array<E?>,
    override var size: Int = 0,
) : MutableBag<E> {
    @Suppress("UNCHECKED_CAST")
    constructor(capacity: Int) : this(java.lang.reflect.Array.newInstance(Any::class.java, capacity) as Array<E?>)

    override fun isEmpty() = size == 0

    override fun contains(element: E): Boolean {
        repeat(size) {
            if (element == data[it])
                return true
        }
        return false
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        elements.forEach { element ->
            if (!contains(element)) return false
        }
        return true
    }

    override fun get(index: Int) = if (index >= data.size) null else data[index]

    private fun grow(newCapacity: Int) {
        data = data.copyOf(newCapacity)
    }

    override fun set(index: Int, element: E) {
        if (index >= data.size)
            grow(max((2 * data.size), index + 1))
        size = max(size, index + 1)
        data[index] = element
    }

    override fun add(element: E): Boolean {
        if (size == data.size)
            grow(data.size * 2)
        data[size++] = element
        return true
    }

    override fun remove(element: E): Boolean {
        repeat(size) {
            if (element == data[it]) {
                data[it] = data[--size]
                data[size] = null
                return true
            }
        }
        return false
    }

    override fun removeAt(index: Int): E {
        val e = data[index]!!
        data[index] = data[--size]
        data[size] = null
        return e
    }

    fun removeLast(): E {
        if (isEmpty()) throw NoSuchElementException()
        val e = data[--size]!!
        data[size] = null
        return e
    }

    override fun addAll(elements: Collection<E>): Boolean {
        elements.forEach { element ->
            add(element)
        }
        return elements.isNotEmpty()
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var modified = false
        elements.forEach { element ->
            if (remove(element)) modified = true
        }
        return modified
    }

    override fun clear() {
        data.fill(null)
        size = 0
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        var modified = false
        val iter = iterator()
        while (iter.hasNext()) {
            if (!elements.contains(iter.next())) {
                modified = true
                iter.remove()
            }
        }
        return modified
    }

    override fun iterator() = BagIterator()

    override fun toString() =
        "Bag(${(0 until size).joinToString { data[it].toString() }})"

    inner class BagIterator : MutableIterator<E> {
        private var cursor = 0
        private var validCursorPos = false

        override fun hasNext() = cursor < size

        override fun next(): E {
            if (cursor == size)
                throw NoSuchElementException()
            val e = data[cursor++]!!
            validCursorPos = true
            return e
        }

        override fun remove() {
            if (!validCursorPos)
                throw IllegalStateException()
            validCursorPos = false
            removeAt(--cursor)
        }
    }
}

fun <E> emptyBag(capacity: Int = 64): MutableBag<E> = BagImpl(capacity)

fun <E> bagOf(elements: Collection<E>): MutableBag<E> = BagImpl<E>(elements.size).apply {
    elements.forEach { add(it) }
}

fun <E> bagOf(vararg elements: E): MutableBag<E> = BagImpl<E>(elements.size).apply {
    elements.forEach { add(it) }
}
