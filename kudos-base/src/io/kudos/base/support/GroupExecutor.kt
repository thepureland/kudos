package io.kudos.base.support

import kotlin.math.ceil

/**
 * 分组执行器
 * 
 * 将集合按指定大小分组，然后对每个分组执行指定的操作。
 * 适用于批量处理大量数据，避免一次性处理导致内存溢出或超时。
 * 
 * 核心功能：
 * 1. 集合分组：将集合按指定大小（groupSize）分成多个子列表
 * 2. 分组执行：对每个分组依次执行操作函数
 * 3. 自动计算：自动计算分组数量，最后一组可能小于groupSize
 * 
 * 使用场景：
 * - 批量数据库更新：避免一次性更新大量数据导致超时
 * - 批量文件处理：避免一次性加载大量文件到内存
 * - 批量API调用：避免一次性发送大量请求导致限流
 * 
 * 分组算法：
 * - 分组数量 = ceil(集合大小 / 每组大小)
 * - 每组起始索引 = 组索引 * 每组大小
 * - 每组结束索引 = 起始索引 + 每组大小（最后一组为集合大小）
 * 
 * 注意事项：
 * - 操作是顺序执行的，不是并行执行
 * - 如果操作抛出异常，会中断后续分组的执行
 * - 最后一组的元素数量可能小于groupSize
 * 
 * @param E 集合元素类型
 */
class GroupExecutor<E>(
    /** 待分组的集合 */
    private val elems: Collection<E>,
    /** 每个分组大小，默认为1000 */
    private val groupSize: Int = 1000,
    /** 每个分组执行的操作，接收分组后的子列表 */
    private val operation: (List<E>) -> Unit
) {

    /**
     * 执行分组操作
     * 
     * 将集合分组后，依次对每个分组执行操作函数。
     * 
     * 工作流程：
     * 1. 计算分组数量：使用ceil向上取整，确保所有元素都被分组
     * 2. 转换为列表：将集合转换为ArrayList，支持subList操作
     * 3. 遍历分组：
     *    - 计算每组的起始和结束索引
     *    - 使用subList获取分组子列表
     *    - 调用operation执行操作
     * 
     * 分组计算：
     * - 分组数量 = ceil(集合大小 / 每组大小)
     * - 第i组的起始索引 = i * 每组大小
     * - 第i组的结束索引 = 起始索引 + 每组大小（最后一组为集合大小）
     * 
     * 边界处理：
     * - 最后一组的结束索引直接使用集合大小，确保包含所有剩余元素
     * - 如果集合为空，不会执行任何操作
     * - 如果groupSize大于集合大小，只会有一个分组
     * 
     * 执行顺序：
     * - 分组按顺序执行，不是并行执行
     * - 如果某个分组的操作抛出异常，会中断后续分组的执行
     * - 每个分组的操作是独立的，互不影响
     * 
     * 性能考虑：
     * - 使用subList避免复制数据，提高性能
     * - 分组大小应根据实际场景调整，平衡内存和性能
     * - 适合处理大量数据的批量操作
     * 
     * 注意事项：
     * - subList返回的是原列表的视图，修改会影响原列表
     * - 如果需要在操作中修改数据，建议先复制子列表
     * - 异常处理应在operation内部完成，避免中断整个流程
     */
    fun execute() {
        val size = elems.size
        val groupCount = ceil(size / groupSize.toDouble()).toInt()
        val elemList: List<E> = ArrayList(elems)
        for (index in 0 until groupCount) {
            val from = index * groupSize
            val end = if (index == groupCount - 1) elemList.size else from + groupSize
            val subList = elemList.subList(from, end)
            operation(subList)
        }
    }


}