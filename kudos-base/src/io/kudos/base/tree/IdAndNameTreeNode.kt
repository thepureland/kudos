package io.kudos.base.tree

/**
 * id和name组成的树节点
 *
 * @author K
 * @since 1.0.0
 */
data class IdAndNameTreeNode<T>(

    /** 惟一标识 */
    val id: T,

    /** 名称 */
    val name: String,

    /** 父id */
    val parentId: T? = null,

    /** 序号 */
    val orderNum: Int? = null,

    /** 孩子结点 */
    val children: MutableList<ITreeNode<T>> = mutableListOf()

) : ITreeNode<T>,Comparable<IdAndNameTreeNode<T>> {

    override fun _getId(): T = id

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