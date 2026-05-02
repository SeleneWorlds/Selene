/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package world.selene.common.event.impl

import com.google.common.annotations.VisibleForTesting
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.IdentityHashMap
import java.util.PriorityQueue

/**
 * Contains a topological sort implementation, with tie breaking using a [Comparator].
 * 
 * 
 * The final order is always deterministic (i.e. doesn't change with the order of the input elements or the edges),
 * assuming that they are all different according to the comparator. This also holds in the presence of cycles.
 * 
 * 
 * The steps are as follows:
 * 
 *  1. Compute node SCCs (Strongly Connected Components, i.e. cycles).
 *  1. Sort nodes within SCCs using the comparator.
 *  1. Sort SCCs with respect to each other by respecting constraints, and using the comparator in case of a tie.
 * 
 */
object NodeSorting {
    private val LOGGER: Logger = LoggerFactory.getLogger(NodeSorting::class.java)

    @VisibleForTesting
    const val ENABLE_CYCLE_WARNING: Boolean = true

    /**
     * Sort a list of nodes.
     * 
     * @param sortedNodes The list of nodes to sort. Will be modified in-place.
     * @param elementDescription A description of the elements, used for logging in the presence of cycles.
     * @param comparator The comparator to break ties and to order elements within a cycle.
     * @return `true` if all the constraints were satisfied, `false` if there was at least one cycle.
     */
    fun <N : SortableNode<N>> sort(
        sortedNodes: MutableList<N>,
        elementDescription: String,
        comparator: Comparator<N>
    ): Boolean {
        // FIRST KOSARAJU SCC VISIT
        val toposort: MutableList<N> = ArrayList(sortedNodes.size)

        for (node in sortedNodes) {
            forwardVisit(node, toposort)
        }

        clearStatus(toposort)
        toposort.reverse()

        // SECOND KOSARAJU SCC VISIT
        val nodeToScc: MutableMap<N, NodeScc<N>> = IdentityHashMap()

        for (node in toposort) {
            if (!node.visited) {
                val sccNodes: MutableList<N> = ArrayList()
                // Collect nodes in SCC.
                backwardVisit(node, sccNodes)
                // Sort nodes by id.
                sccNodes.sortWith(comparator)
                // Mark nodes as belonging to this SCC.
                val scc = NodeScc(sccNodes)

                for (nodeInScc in sccNodes) {
                    nodeToScc[nodeInScc] = scc
                }
            }
        }

        clearStatus(toposort)

        // Build SCC graph
        for (scc in nodeToScc.values) {
            for (node in scc.nodes) {
                for (subsequentNode in node.subsequentNodes) {
                    val subsequentScc = nodeToScc[subsequentNode]!!

                    if (subsequentScc !== scc) {
                        scc.subsequentSccs.add(subsequentScc)
                        subsequentScc.inDegree++
                    }
                }
            }
        }

        // Order SCCs according to priorities. When there is a choice, use the SCC with the lowest id.
        // The priority queue contains all SCCs that currently have 0 in-degree.
        val pq: PriorityQueue<NodeScc<N>> = PriorityQueue<NodeScc<N>>(
            Comparator.comparing(
                { scc: NodeScc<N> -> scc.nodes.first() },
                comparator
            )
        )
        sortedNodes.clear()

        for (scc in nodeToScc.values) {
            if (scc.inDegree == 0) {
                pq.add(scc)
                // Prevent adding the same SCC multiple times, as nodeToScc may contain the same value multiple times.
                scc.inDegree = -1
            }
        }

        var noCycle = true

        while (!pq.isEmpty()) {
            val scc = pq.poll()
            sortedNodes.addAll(scc.nodes)

            if (scc.nodes.size > 1) {
                noCycle = false

                if (ENABLE_CYCLE_WARNING) {
                    // Print cycle warning
                    val builder = StringBuilder()
                    builder.append("Found cycle while sorting ").append(elementDescription).append(":\n")

                    for (node in scc.nodes) {
                        builder.append("\t").append(node.description).append("\n")
                    }

                    LOGGER.warn(builder.toString())
                }
            }

            for (subsequentScc in scc.subsequentSccs) {
                subsequentScc.inDegree--

                if (subsequentScc.inDegree == 0) {
                    pq.add(subsequentScc)
                }
            }
        }

        return noCycle
    }

    private fun <N : SortableNode<N>> forwardVisit(node: N, toposort: MutableList<N>) {
        if (!node.visited) {
            // Not yet visited.
            node.visited = true

            for (data in node.subsequentNodes) {
                forwardVisit(data, toposort)
            }

            toposort.add(node)
        }
    }

    private fun <N : SortableNode<N>> clearStatus(nodes: MutableList<N>) {
        for (node in nodes) {
            node.visited = false
        }
    }

    private fun <N : SortableNode<N>> backwardVisit(node: N, sccNodes: MutableList<N>) {
        if (!node.visited) {
            node.visited = true
            sccNodes.add(node)

            for (data in node.previousNodes) {
                backwardVisit(data, sccNodes)
            }
        }
    }

    private class NodeScc<N : SortableNode<N>>(val nodes: MutableList<N>) {
        val subsequentSccs: MutableList<NodeScc<N>> = ArrayList<NodeScc<N>>()
        var inDegree: Int = 0
    }
}
