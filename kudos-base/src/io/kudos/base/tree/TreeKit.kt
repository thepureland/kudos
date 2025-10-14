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
     * @return List(树根结点)
     * @author K
     * @since 1.0.0
     */
    fun <T, E : ITreeNode<T>> convertListToTree(
        treeNodeList: List<E>,
        direction: DirectionEnum? = null,
        callback: ICallback<E, Unit>? = null
    ): List<E> {
        return ListToTreeConverter.convert(treeNodeList, direction, callback)
    }

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
     * @author ChatGPT
     * @author K
     * @since 1.0.0
     */
    fun <T, R> breadthTraverse(rootNode: ITreeNode<T>, callback: ICallback<ITreeNode<T>, R>) {
        val queue: ArrayDeque<ITreeNode<T>> = ArrayDeque()
        val visited = HashSet<T>() // 防御“伪树”里可能出现的环
        queue.addLast(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (!visited.add(node._getId())) continue

            // 执行回调；忽略其返回值
            callback.execute(node)

            // 按当前顺序把孩子入队，保证同层从左到右
            val children = node._getChildren()
            if (children.isNotEmpty()) {
                for (child in children) queue.addLast(child)
            }
        }
    }

    private fun <T, R> depth(node: ITreeNode<T>, callback: ICallback<ITreeNode<T>, R>) {
        callback.execute(node)
        val children = node._getChildren()
        if (children.isNotEmpty()) {
            for (subNode in children) {
                depth(subNode, callback)
            }
        }
    }

}