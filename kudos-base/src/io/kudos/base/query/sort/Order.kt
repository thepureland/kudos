package io.kudos.base.query.sort

import java.io.Serializable

/**
 * 单个排序规则封装类
 * 
 * 用于封装一个属性的排序规则，包括属性名和排序方向。
 * 
 * 核心属性：
 * - property：要排序的属性名（字段名）
 * - direction：排序方向（ASC升序，DESC降序）
 * 
 * 排序方向：
 * - ASC：升序排列，从小到大
 * - DESC：降序排列，从大到小
 * 
 * 使用场景：
 * - 数据库查询的排序
 * - 列表数据的排序
 * - 配合Sort类使用，实现多属性排序
 * 
 * 注意事项：
 * - 属性名用于生成SQL的ORDER BY子句
 * - 支持链式调用和静态工厂方法
 * - 实现Serializable接口，支持序列化
 * 
 * @since 1.0.0
 */
data class Order(
    var property: String = "",
    var direction: DirectionEnum = DirectionEnum.ASC
) : Serializable {

    fun isAscending() = direction == DirectionEnum.ASC

    override fun toString() = "${property}: $direction"

    companion object {
        private const val serialVersionUID = 1522511010900998988L
        fun asc(property: String): Order = Order(property)
        fun desc(property: String): Order = Order(property, DirectionEnum.DESC)
    }
}