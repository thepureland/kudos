package io.kudos.base.tree

import io.kudos.base.logger.LogFactory
import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.support.ICallback

/**
 * List-to-tree converter.
 *
 * Converts a flat node list into a tree structure, supporting parent-child relationship building, sorting, and callback handling.
 *
 * Core features:
 * 1. Tree building: builds the tree based on parent-child relationships among nodes
 * 2. Root node identification: automatically identifies nodes whose parent ID is null or empty as roots
 * 3. Sorting support: supports sorting all levels of the tree (ASC/DESC)
 * 4. Callback mechanism: supports callback handling after a node is attached
 *
 * Workflow:
 * 1. Build the node map: create an ID-to-node map for fast lookups
 * 2. Identify roots: find nodes whose parent ID is null or empty
 * 3. Establish parent-child relationships: attach child nodes under their parents
 * 4. Execute callbacks: invoke the callback for each root node
 * 5. Sort: if a sort direction is specified, recursively sort the tree
 *
 * Node requirements:
 * - Nodes must implement the ITreeNode interface
 * - Nodes must provide ID and parent ID information
 * - If sorting is specified, the node type must implement the Comparable interface
 *
 * Data integrity:
 * - If a node's parent does not exist, a warning is logged
 * - The node is ignored and not added to the tree
 * - No exception is thrown, ensuring the robustness of the conversion process
 *
 * Use cases:
 * - Building tree-shaped data such as menu trees and organizational hierarchies
 * - Converting flat data into a tree structure
 * - Displaying sorted tree data
 *
 * Notes:
 * - Parent nodes must be present in the list; otherwise child nodes are ignored
 * - Sorting is applied recursively to all node levels
 * - Callbacks are executed after the node has been attached
 * - Sorting reorders the children collection of each node in place (side effect)
 *
 * @since 1.0.0
 */
object ListToTreeConverter {

    private val LOG = LogFactory.getLog(ListToTreeConverter::class)


    /**
     * Converts a list structure into a tree structure.
     *
     * Converts a flat node list into a tree structure, with support for sorting and callbacks.
     *
     * Workflow:
     * 1. Build the node map: create an ID-to-node map for fast lookups
     * 2. Identify roots: iterate over all nodes and treat those with a null or empty parent ID as roots
     * 3. Build the tree:
     *    - For each node, look up its parent
     *    - If the parent exists, append the current node to the parent's child list
     *    - If the parent does not exist, log a warning (data may be incomplete)
     * 4. Execute callbacks: invoke the callback for each root node
     * 5. Sort: if a sort direction is specified, sort the tree
     *
     * Node identification:
     * - Root node: parent ID is null or empty string
     * - Child node: parent ID is not empty and the corresponding parent can be found in the map
     *
     * Sorting support:
     * - If direction is specified, the tree is sorted
     * - Sorting requires the node type to implement the Comparable interface
     * - Sorting is applied recursively to all node levels
     *
     * Callback mechanism:
     * - For each root node, callback.execute(node) is invoked
     * - The callback is invoked after the node has been attached
     * - Useful for additional processing after the node is attached
     *
     * Data integrity:
     * - If a node's parent does not exist, a warning is logged
     * - The node is ignored and not added to the tree
     * - No exception is thrown, ensuring the robustness of the conversion process
     *
     * Notes:
     * - Nodes in the node list must implement the ITreeNode interface
     * - If sorting is specified, the node type must implement the Comparable interface
     * - Parent nodes must be present in the list; otherwise child nodes are ignored
     *
     * @param T Node unique identifier type
     * @param E Tree node type; must implement the ITreeNode interface
     * @param treeNodeList List of node objects
     * @param direction Sort direction; if specified, the node must implement Comparable; null means no sorting
     * @param callback Callback invoked after a node is attached; may be null
     * @param strict Strict mode: when true, an "orphan" node whose parentId cannot be found in the list throws
     *               [IllegalArgumentException]; when false (default), it logs a WARN and silently drops the node,
     *               preserving backward compatibility. Scenarios that need to guarantee complete input data should explicitly pass true.
     * @return List of root tree nodes
     * @throws IllegalArgumentException When strict=true and orphan nodes exist
     */
    fun <T, E : ITreeNode<T>> convert(
        treeNodeList: List<E>,
        direction: DirectionEnum? = null,
        callback: ICallback<E, Unit>? = null,
        strict: Boolean = false
    ): List<E> {
        val treeNodeMap = treeNodeList.associateByTo(HashMap(treeNodeList.size, 1f)) { it._getId() }
        val nodeList = mutableListOf<E>()
        for (node in treeNodeList) {
            val pId = node._getParentId()
            if (pId == null || (pId is CharSequence && pId.isEmpty())) { // Root
                nodeList.add(node)
                callback?.execute(node)
                continue
            }
            val pNode = treeNodeMap[pId]
            if (pNode != null) {
                pNode._getChildren().add(node)
            } else {
                require(!strict) { "Parent node #${pId} of node #${node._getId()} is not in the input list (strict mode)" }
                LOG.warn("Parent node #${pId} of node #${node._getId()} does not exist!")
            }
        }

        // Sort
        return if (direction != null && treeNodeList.isNotEmpty()) sort(nodeList, direction) else nodeList
    }

    /**
     * Sorts tree nodes.
     *
     * Recursively sorts all levels of the tree, supporting ascending and descending order.
     *
     * Workflow:
     * 1. Type check: verify that the node type implements the Comparable interface
     * 2. Sort the roots: sort the nodes at the current level
     * 3. Recursive sort: recursively invoke sort on each node's child list
     * 4. Update children: replace each node's child list with the sorted result
     *
     * Sorting rules:
     * - Uses the node's Comparable interface for sorting
     * - ASC: ascending order
     * - DESC: descending order (implemented by reversing)
     *
     * Recursive processing:
     * - Recursively invokes sort on each node's child list
     * - Ensures that nodes at all levels are sorted
     * - The sorted child list replaces the original list
     *
     * Type safety:
     * - Checks at runtime whether the node implements the Comparable interface
     * - If it does not, an exception is thrown
     *
     * Notes:
     * - The node type must implement the Comparable interface
     * - Sorting modifies the order of the original node list
     * - Sorting is applied recursively to all child nodes
     *
     * @param T Node unique identifier type
     * @param E Tree node type
     * @param nodes List of nodes to sort
     * @param direction Sort direction (ASC or DESC)
     * @return Sorted node list
     * @throws IllegalStateException If the node type does not implement the Comparable interface
     */
    private fun <T, E : ITreeNode<T>> sort(nodes: List<E>, direction: DirectionEnum): List<E> {
        if (nodes.isEmpty()) return nodes
        val sortedRoots = sortByComparable(nodes, direction)
        sortedRoots.forEach { sortChildrenInPlace(it._getChildren(), direction) }
        return sortedRoots
    }

    private fun <T> sortChildrenInPlace(children: MutableList<ITreeNode<T>>, direction: DirectionEnum) {
        if (children.isEmpty()) return
        val sorted = sortByComparable(children, direction)
        children.clear()
        children.addAll(sorted)
        sorted.forEach { sortChildrenInPlace(it._getChildren(), direction) }
    }

    /**
     * Sorts by each element's own [Comparable] implementation, avoiding reflective calls to compareTo.
     *
     * Requires **all elements to implement Comparable** (a guard check on the first element is sufficient — if the first
     * element is not Comparable, the batch must be heterogeneous, so [error] is thrown early; the homogeneity guarantee
     * makes the subsequent cast safe).
     *
     * @param T Element type
     * @param items Collection to sort
     * @param direction ASC / DESC
     * @return Sorted list
     * @throws IllegalStateException If an element does not implement Comparable
     * @author K
     * @since 1.0.0
     */
    private fun <T : Any> sortByComparable(items: List<T>, direction: DirectionEnum): List<T> {
        if (items.first() !is Comparable<*>) {
            error("Class ${items.first()::class.simpleName} must implement the Comparable interface!")
        }
        // Call directly via Comparable's JVM bridge method to avoid reflection;
        // the cast is guarded above by `is Comparable<*>`, so it is guaranteed to be valid at runtime.
        @Suppress("UNCHECKED_CAST")
        val comparator = Comparator<T> { a, b -> (a as Comparable<Any>).compareTo(b) }
        return items.sortedWith(if (direction == DirectionEnum.ASC) comparator else comparator.reversed())
    }

}