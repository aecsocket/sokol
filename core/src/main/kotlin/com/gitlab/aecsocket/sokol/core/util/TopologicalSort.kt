package com.gitlab.aecsocket.sokol.core.util

import com.google.common.collect.AbstractIterator
import com.google.common.graph.Graph
import java.util.*

// https://github.com/jrtom/jung/pull/174/files

@Suppress("UnstableApiUsage")
fun <N> Graph<N>.topologicallySorted() = object : Iterable<N> {
    override fun iterator() = object : AbstractIterator<N>() {
        val roots: MutableList<N> = nodes().filter { inDegree(it) == 0 }.toMutableList()
        val nonRootsToInDegree: MutableMap<N, Int> = nodes()
            .filter { inDegree(it) > 0 }
            .associateWith { inDegree(it) }
            .toMutableMap()

        override fun computeNext(): N? {
            // Kahn's algorithm
            if (roots.isNotEmpty()) {
                val next = roots.removeFirst()
                successors(next).forEach { successor ->
                    val newInDegree = nonRootsToInDegree[successor]!! - 1
                    nonRootsToInDegree[successor] = newInDegree
                    if (newInDegree == 0) {
                        nonRootsToInDegree.remove(successor)
                        roots.add(successor)
                    }
                }
                return next
            }
            if (nonRootsToInDegree.isEmpty())
                throw IllegalStateException("Graph has at least one cycle")
            return endOfData()
        }
    }
}
