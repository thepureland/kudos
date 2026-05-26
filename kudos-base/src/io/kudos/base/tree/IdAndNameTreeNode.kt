package io.kudos.base.tree

/**
 * Tree node composed of an id and a name.
 *
 * @author K
 * @since 1.0.0
 */
data class IdAndNameTreeNode<T>(

    /** Unique identifier */
    val id: T? = null,

    /** Name */
    val name: String,

    /** Parent id */
    val parentId: T? = null,

    /** Order number */
    val orderNum: Int? = null,

    /** Child nodes */
    val children: MutableList<ITreeNode<T>> = mutableListOf()

) : ITreeNode<T>,Comparable<IdAndNameTreeNode<T>> {

    constructor(): this(null, "")

    override fun _getId(): T = id!!

    override fun _getParentId(): T? = parentId

    override fun _getChildren(): MutableList<ITreeNode<T>> = children

    override fun compareTo(other: IdAndNameTreeNode<T>): Int {
        return if (orderNum != null && other.orderNum != null) {
            orderNum.compareTo(other.orderNum)
        } else {
            name.compareTo(other.name)
        }
    }

}