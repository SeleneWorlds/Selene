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

abstract class SortableNode<N : SortableNode<N>> {
    val subsequentNodes = mutableListOf<N>()
    val previousNodes = mutableListOf<N>()
    var visited: Boolean = false

    /**
     * @return Description of this node, used to print the cycle warning.
     */
    abstract val description: String?

    protected fun addSubsequentNode(node: N) {
        this.subsequentNodes.add(node)
    }

    protected fun addPreviousNode(node: N) {
        this.previousNodes.add(node)
    }

    companion object {
        fun <N : SortableNode<N>> link(first: N, second: N) {
            require(first !== second) { "Cannot link a node to itself!" }

            first.addSubsequentNode(second)
            second.addPreviousNode(first)
        }
    }
}
