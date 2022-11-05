package com.gitlab.aecsocket.sokol.core.util

import kotlin.math.min

class Bits private constructor(
    private var words: LongArray,
    // more of an "is definitely empty"
    private var isEmpty: Boolean
) : Iterable<Boolean> {
    constructor(capacity: Int = 64) : this(longArrayOf(0), true) {
        ensureCapacity(capacity)
    }

    constructor(bits: Bits) : this(bits.words.clone(), bits.isEmpty)

    val size: Int get() {
        val bits = words
        (bits.size - 1 downTo 0).forEach { word ->
            val wordBits = bits[word]
            if (wordBits != 0L)
                return (word shl 6) + 64 - wordBits.countLeadingZeroBits()
        }
        return 0
    }

    operator fun get(index: Int): Boolean {
        if (isEmpty) return false
        val word = index shr 6
        return word < words.size && (words[word] and (1L shl index)) != 0L
    }

    private fun ensureCapacity(len: Int) {
        if (len >= words.size) {
            words = words.copyOf(len + 1)
        }
    }

    fun set(index: Int) {
        isEmpty = false
        val word = index shr 6
        ensureCapacity(word)
        words[word] = words[word] or (1L shl index)
    }

    fun clear(index: Int) {
        val word = index shr 6
        if (word >= words.size) return
        words[word] = words[word] and (1L shl index).inv()
    }

    operator fun set(index: Int, value: Boolean) {
        if (value) {
            isEmpty = false
            set(index)
        }
        else clear(index)
    }

    fun clear() {
        isEmpty = true
        words.fill(0)
    }

    fun isEmpty(): Boolean {
        if (isEmpty) return true
        words.forEach { word ->
            if (word != 0L) return false
        }
        return true
    }

    fun isNotEmpty() = !isEmpty()

    fun containsAll(other: Bits): Boolean {
        if (other.isEmpty) return true
        val bits = words
        val otherBits = other.words
        val bitsSize = bits.size
        val otherBitsSize = otherBits.size

        (bitsSize until otherBitsSize).forEach {
            if (otherBits[it] != 0L) return false
        }

        repeat(min(bitsSize, otherBitsSize)) {
            if ((bits[it] and otherBits[it]) != otherBits[it]) return false
        }

        return true
    }

    fun intersects(other: Bits): Boolean {
        if (isEmpty || other.isEmpty) return false
        val bits = words
        val otherBits = other.words
        repeat(min(bits.size, otherBits.size)) {
            if ((bits[it] and otherBits[it]) != 0L) return true
        }
        return false
    }

    // todo make better
    override fun toString() = words.contentToString()

    inner class BitsIterator : Iterator<Boolean> {
        private var cursor = 0
        private val size = this@Bits.size

        override fun hasNext() = cursor < size

        override fun next(): Boolean {
            if (cursor == size)
                throw NoSuchElementException()
            return get(cursor++)
        }
    }

    override fun iterator() = BitsIterator()
}

fun emptyBits() = Bits(0)
