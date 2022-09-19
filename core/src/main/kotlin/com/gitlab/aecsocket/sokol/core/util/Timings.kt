package com.gitlab.aecsocket.sokol.core.util

class Timings(
    val maxMeasurements: Int,
    timings: List<Long> = emptyList(),
) {
    private val _timings = timings.toMutableList()
    val timings: List<Long> get() = _timings

    fun last() = _timings.last()

    fun average() = _timings.average()

    fun min() = _timings.min()

    fun max() = _timings.max()

    fun add(timing: Long) {
        _timings.add(timing)
        while (_timings.size > maxMeasurements) {
            _timings.removeAt(0)
        }
    }

    fun <R> time(action: () -> R): R {
        val start = System.currentTimeMillis()
        val result = action()
        val delta = System.currentTimeMillis() - start
        add(delta)
        return result
    }

    fun takeLast(n: Int) = Timings(maxMeasurements, timings.takeLast(n))
}
