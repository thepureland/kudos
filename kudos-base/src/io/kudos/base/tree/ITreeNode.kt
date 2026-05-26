package io.kudos.base.tree

import java.beans.Transient
import java.io.Serializable

/**
 * Tree node interface.
 *
 * @param T Node identifier type
 * @author K
 * @since 1.0.0
 */
interface ITreeNode<T> : Serializable {

    /**
     * Returns the unique identifier of the current node.
     *
     * @author K
     * @since 1.0.0
     */
    @Transient
    fun _getId(): T

    /**
     * Returns the unique identifier of the parent node.
     *
     * @author K
     * @since 1.0.0
     */
    @Transient
    fun _getParentId(): T?

    /**
     * Returns the child nodes.
     * A complete tree can in fact be built from id and parentId alone; this property is introduced merely for user convenience.
     * Users may override this method as needed and return the corresponding property value.
     * If TreeKit::convertListToTree() is used, the child nodes are filled in automatically and there is no need for the user to maintain parent-child relationships.
     *
     * @return List of child nodes
     * @author K
     * @since 1.0.0
     */
    @Transient
    fun _getChildren(): MutableList<ITreeNode<T>> = mutableListOf()

}