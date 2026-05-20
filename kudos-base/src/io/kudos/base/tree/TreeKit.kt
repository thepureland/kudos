package io.kudos.base.tree

import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.support.ICallback

/**
 * 树操作工具类
 *
 * @author K
 * @since 1.0.0
 */
object TreeKit {

    /**
     * 将列表结构转为树结构
     *
     * @param T 树结点惟一标识的类型
     * @param E 树结点类型
     * @param treeNodeList 结点对象列表
     * @param direction 排序，指定排序时E必须实现Comparable接口，为null将不做排序，默认为null
     * @param callback 结点挂载后的回调
     * @param strict 严格模式：true 时遇到孤儿结点（parentId 在列表里找不到）会抛
     *               [IllegalArgumentException]；false（默认）按现有行为记 WARN 静默丢弃
     * @return List(树根结点)
     * @throws IllegalArgumentException 当 strict=true 且存在孤儿结点时
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
     * 深度优先遍历树，并执行回调
     *
     * @param T 树结点惟一标识的类型
     * @param R 回调返回值类型
     * @param rootNode 树的根结点
     * @param callback 回调函数
     * @author K
     * @since 1.0.0
     */
    fun <T, R> depthTraverse(rootNode: ITreeNode<T>, callback: ICallback<ITreeNode<T>, R>) {
        depth(rootNode, callback)
    }

    /**
     * 广度优先遍历树，并执行回调（不收集返回值）
     *
     * @param T 树结点惟一标识的类型
     * @param R 回调返回值类型（此方法不返回；仅执行回调）
     * @param rootNode 根结点
     * @param callback 每访问到一个结点就调用一次
     * @author AI: ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun <T, R> breadthTraverse(rootNode: ITreeNode<T>, callback: ICallback<ITreeNode<T>, R>) {
        val queue = ArrayDeque<ITreeNode<T>>().apply { addLast(rootNode) }
        val visited = mutableSetOf<T>() // 防御“伪树”里可能出现的环
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (!visited.add(node._getId())) continue
            // 执行回调；忽略其返回值
            callback.execute(node)
            // 按当前顺序把孩子入队，保证同层从左到右
            queue.addAll(node._getChildren())
        }
    }

    /**
     * 深度优先递归：先对当前节点执行 callback，再 DFS 进入每个孩子。
     * 与广度优先版本（[breadth]）相对——树形 print / 拷贝场景用 DFS 更直观。
     *
     * @param T 树节点的载荷类型
     * @param R callback 的返回类型（被忽略，仅作签名占位）
     * @param node 当前节点
     * @param callback 每个节点上的回调
     * @author K
     * @since 1.0.0
     */
    private fun <T, R> depth(node: ITreeNode<T>, callback: ICallback<ITreeNode<T>, R>) {
        callback.execute(node)
        node._getChildren().forEach { depth(it, callback) }
    }

}