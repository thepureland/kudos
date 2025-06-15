package io.kudos.base.tree

/**
 * 测试用数结点
 *
 * @author K
 * @since 1.0.0
 */
internal class TestTreeNode : ITreeNode<String> {
    var id: String
        private set
    var parentId: String? = null
        private set
    var name: String? = null
        private set
    var children = mutableListOf<ITreeNode<String>>()

    constructor(id: String, parentId: String?, name: String?) : super() {
        this.id = id
        this.parentId = parentId
        this.name = name
    }

    companion object Companion {
        private const val serialVersionUID = -3832151541461087421L
    }

    override fun _getId(): String = id

    override fun _getParentId(): String? = parentId

    override fun _getChildren(): MutableList<ITreeNode<String>> {
        return children
    }

}