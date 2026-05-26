package io.kudos.base.tree

import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.support.ICallback

/**
 * Tree operation utility class.
 *
 * @author K
 * @since 1.0.0
 */
object TreeKit {

    /**
     * Converts a list structure into a tree structure.
     *
     * @param T Tree node unique identifier type
     * @param E Tree node type
     * @param treeNodeList List of node objects
     * @param direction Sort order; when specified, E must implement the Comparable interface; null means no sorting; defaults to null
     * @param callback Callback invoked after a node is attached
     * @param strict Strict mode: when true, an orphan node (whose parentId cannot be found in the list) causes
     *               [IllegalArgumentException] to be thrown; when false (default), it logs a WARN and silently drops the node
     * @return List(tree root nodes)
     * @throws IllegalArgumentException When strict=true and orphan nodes exist
     * @author K
     * @since 1.0.0
     */
    fun <T, E : ITreeNode<T>> convertListToTree(
        treeNodeList: List<E>,
        direction: DirectionEnum? = null,
        callback: ICallback<E, Unit>? = null,
        strict: Boolean = false
    ): List<E> = ListToTreeConverter.convert(treeNodeList, direction, callback, strict)

    /**
     * Depth-first traverses the tree and invokes the callback.
     *
     * @param T Tree node unique identifier type
     * @param R Callback return value type
     * @param rootNode Root node of the tree
     * @param callback Callback function
     * @author K
     * @since 1.0.0
     */
    fun <T, R> depthTraverse(rootNode: ITreeNode<T>, callback: ICallback<ITreeNode<T>, R>) {
        depth(rootNode, callback)
    }

    /**
     * Breadth-first traverses the tree and invokes the callback (return values are not collected).
     *
     * @param T Tree node unique identifier type
     * @param R Callback return value type (this method does not return; it only invokes the callback)
     * @param rootNode Root node
     * @param callback Invoked once for each visited node
     * @author AI: ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun <T, R> breadthTraverse(rootNode: ITreeNode<T>, callback: ICallback<ITreeNode<T>, R>) {
        val queue = ArrayDeque<ITreeNode<T>>().apply { addLast(rootNode) }
        val visited = mutableSetOf<T>() // Guard against cycles that may appear in "pseudo-trees"
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (!visited.add(node._getId())) continue
            // Invoke the callback; ignore its return value
            callback.execute(node)
            // Enqueue children in their current order to keep the same-level order left-to-right
            queue.addAll(node._getChildren())
        }
    }

    /**
     * Depth-first recursion: first invoke the callback on the current node, then DFS into each child.
     * The counterpart of the breadth-first version ([breadth]); DFS is more intuitive for tree-shaped print/copy scenarios.
     *
     * @param T Tree node payload type
     * @param R Callback return type (ignored; only serves as a signature placeholder)
     * @param node Current node
     * @param callback Callback to invoke at each node
     * @author K
     * @since 1.0.0
     */
    private fun <T, R> depth(node: ITreeNode<T>, callback: ICallback<ITreeNode<T>, R>) {
        callback.execute(node)
        node._getChildren().forEach { depth(it, callback) }
    }

}