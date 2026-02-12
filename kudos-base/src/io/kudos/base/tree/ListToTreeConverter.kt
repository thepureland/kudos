package io.kudos.base.tree

import io.kudos.base.logger.LogFactory
import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.support.ICallback

/**
 * 列表到树结构转换器
 * 
 * 将扁平的节点列表转换为树形结构，支持父子关系的建立、排序和回调处理。
 * 
 * 核心功能：
 * 1. 树结构构建：根据节点的父子关系构建树形结构
 * 2. 根节点识别：自动识别父ID为空或空字符串的节点作为根节点
 * 3. 排序支持：支持对树的所有层级进行排序（ASC/DESC）
 * 4. 回调机制：支持节点挂载后的回调处理
 * 
 * 工作流程：
 * 1. 构建节点映射：创建ID到节点的映射表，便于快速查找
 * 2. 识别根节点：找出父ID为空或空字符串的节点
 * 3. 建立父子关系：将子节点挂载到对应的父节点下
 * 4. 执行回调：对每个根节点执行回调函数
 * 5. 排序处理：如果指定了排序方向，对树进行递归排序
 * 
 * 节点要求：
 * - 节点必须实现ITreeNode接口
 * - 节点需要提供ID和父ID信息
 * - 如果指定排序，节点类型必须实现Comparable接口
 * 
 * 数据完整性：
 * - 如果节点的父节点不存在，会记录警告日志
 * - 该节点会被忽略，不会添加到树中
 * - 不会抛出异常，保证转换过程的健壮性
 * 
 * 使用场景：
 * - 菜单树、组织架构树等树形数据的构建
 * - 扁平数据到树形结构的转换
 * - 需要排序的树形数据展示
 * 
 * 注意事项：
 * - 父节点必须在列表中，否则子节点会被忽略
 * - 排序会递归应用到所有层级的节点
 * - 回调在节点挂载完成后执行
 * - 排序时会就地重排每个节点的children集合（有副作用）
 * 
 * @since 1.0.0
 */
object ListToTreeConverter {

    private val LOG = LogFactory.getLog(ListToTreeConverter::class)


    /**
     * 将列表结构转为树结构
     * 
     * 将扁平的节点列表转换为树形结构，支持排序和回调。
     * 
     * 工作流程：
     * 1. 构建节点映射：创建ID到节点的映射表，便于快速查找
     * 2. 识别根节点：遍历所有节点，找出父ID为空或空字符串的节点作为根节点
     * 3. 构建树结构：
     *    - 对于每个节点，查找其父节点
     *    - 如果父节点存在，将当前节点添加到父节点的子节点列表
     *    - 如果父节点不存在，记录警告日志（可能是数据不完整）
     * 4. 执行回调：对于每个根节点，执行回调函数
     * 5. 排序处理：如果指定了排序方向，对树进行排序
     * 
     * 节点识别：
     * - 根节点：父ID为null或空字符串
     * - 子节点：父ID不为空，且能在映射表中找到对应的父节点
     * 
     * 排序支持：
     * - 如果指定了direction，会对树进行排序
     * - 排序要求节点类型实现Comparable接口
     * - 排序会递归应用到所有层级的节点
     * 
     * 回调机制：
     * - 对于每个根节点，会调用callback.execute(node)
     * - 回调在节点挂载完成后执行
     * - 可以用于节点挂载后的额外处理
     * 
     * 数据完整性：
     * - 如果节点的父节点不存在，会记录警告日志
     * - 该节点会被忽略，不会添加到树中
     * - 不会抛出异常，保证转换过程的健壮性
     * 
     * 注意事项：
     * - 节点列表中的节点必须实现ITreeNode接口
     * - 如果指定排序，节点类型必须实现Comparable接口
     * - 父节点必须在列表中，否则子节点会被忽略
     * 
     * @param T 节点唯一标识的类型
     * @param E 树节点类型，必须实现ITreeNode接口
     * @param treeNodeList 节点对象列表
     * @param direction 排序方向，如果指定则节点必须实现Comparable接口，为null则不排序
     * @param callback 节点挂载后的回调函数，可以为null
     * @return 树根节点列表
     */
    fun <T, E : ITreeNode<T>> convert(
        treeNodeList: List<E>,
        direction: DirectionEnum? = null,
        callback: ICallback<E, Unit>? = null
    ): List<E> {
        val treeNodeMap = HashMap<T, E>(treeNodeList.size, 1f)
        for (obj in treeNodeList) {
            treeNodeMap[obj._getId()] = obj
        }
        val nodeList = ArrayList<E>()
        for (obj in treeNodeList) {
            val node = obj
            val pId = obj._getParentId()
            if (pId == null || (pId is CharSequence && pId.isEmpty())) { // 根
                nodeList.add(node)
                callback?.execute(node)
            } else {
                val pNode = treeNodeMap[pId]
                if (pNode != null) { // 存在父结点
                    pNode._getChildren().add(node)
                } else {
                    LOG.warn("结点#${node._getId()}的父结点#${pId}不存在！")
                }
            }
        }

        // 排序
        if (direction != null && treeNodeList.isNotEmpty()) {
            return sort(nodeList, direction)
        }

        return nodeList
    }

    /**
     * 对树节点进行排序
     * 
     * 递归地对树的所有层级进行排序，支持升序和降序。
     * 
     * 工作流程：
     * 1. 类型检查：检查节点类型是否实现Comparable接口
     * 2. 排序根节点：对当前层级的节点进行排序
     * 3. 递归排序：对每个节点的子节点递归调用sort方法
     * 4. 更新子节点：将排序后的子节点列表更新回节点
     * 
     * 排序规则：
     * - 使用节点的Comparable接口进行排序
     * - ASC：升序排列
     * - DESC：降序排列（通过取反实现）
     * 
     * 递归处理：
     * - 对每个节点的子节点列表递归调用sort
     * - 确保所有层级的节点都被排序
     * - 排序后的子节点列表会替换原来的列表
     * 
     * 类型安全：
     * - 使用@Suppress("UNCHECKED_CAST")抑制类型转换警告
     * - 在运行时检查节点是否实现Comparable接口
     * - 如果未实现，会抛出异常
     * 
     * 注意事项：
     * - 节点类型必须实现Comparable接口
     * - 排序会修改原节点列表的顺序
     * - 排序会递归应用到所有子节点
     * 
     * @param T 节点唯一标识的类型
     * @param E 树节点类型
     * @param nodes 待排序的节点列表
     * @param direction 排序方向（ASC或DESC）
     * @return 排序后的节点列表
     * @throws IllegalStateException 如果节点类型未实现Comparable接口
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T, E : ITreeNode<T>> sort(nodes: List<E>, direction: DirectionEnum): List<E> {
        if (nodes.isEmpty()) {
            return nodes
        }
        if (nodes.first() !is Comparable<*>) {
            error("类${nodes.first()::class.simpleName}必须实现Comparable接口！")
        }

        val nodeList = nodes.sortedWith { o1, o2 ->
            val result = (o1 as Comparable<E>).compareTo(o2)
            if (direction == DirectionEnum.ASC) result else 0 - result
        }

        nodeList.forEach {
            val origChildren = it._getChildren()
            if (origChildren.isNotEmpty()) {
                val children = sortChildren(origChildren, direction)
                origChildren.clear()
                origChildren.addAll(children)
            }
        }

        return nodeList
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> sortChildren(nodes: List<ITreeNode<T>>, direction: DirectionEnum): List<ITreeNode<T>> {
        if (nodes.isEmpty()) {
            return nodes
        }
        if (nodes.first() !is Comparable<*>) {
            error("类${nodes.first()::class.simpleName}必须实现Comparable接口！")
        }

        val sorted = nodes.sortedWith { o1, o2 ->
            val result = (o1 as Comparable<ITreeNode<T>>).compareTo(o2)
            if (direction == DirectionEnum.ASC) result else 0 - result
        }
        sorted.forEach { child ->
            val grandChildren = child._getChildren()
            if (grandChildren.isNotEmpty()) {
                val sortedGrandChildren = sortChildren(grandChildren, direction)
                grandChildren.clear()
                grandChildren.addAll(sortedGrandChildren)
            }
        }
        return sorted
    }

}