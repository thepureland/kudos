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
class Order : Serializable {

    var property: String = ""
    var direction: DirectionEnum = DirectionEnum.ASC

    constructor()

    constructor(property: String, direction: DirectionEnum = DirectionEnum.ASC) {
        this.property = property
        this.direction = direction
    }

    fun isAscending() = direction == DirectionEnum.ASC

    override fun toString() = "${property}: $direction"

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + direction.hashCode()
        result = prime * result + property.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val order = other as Order
        if (direction !== order.direction) {
            return false
        }
        if (property != order.property) {
            return false
        }
        return true
    }

    companion object {
        private const val serialVersionUID = 1522511010900998988L
        fun asc(property: String): Order {
            return Order(property)
        }

        fun desc(property: String): Order {
            return Order(property, DirectionEnum.DESC)
        }
    }
}