package io.kudos.base.query.sort

import io.kudos.base.lang.string.humpToUnderscore
import java.io.Serializable

/**
 * 排序规则封装类
 * 
 * 用于封装多个排序规则，支持多属性排序和排序方向的组合。
 * 
 * 核心功能：
 * 1. 多属性排序：支持按多个属性进行排序
 * 2. 排序方向：每个属性可以指定独立的排序方向（ASC/DESC）
 * 3. 排序合并：支持通过and方法合并多个Sort对象
 * 4. SQL生成：支持将排序规则转换为SQL的ORDER BY子句
 * 
 * 数据结构：
 * - orders：排序规则列表，按添加顺序执行排序
 * - 每个Order包含属性名和排序方向
 * 
 * 排序执行顺序：
 * - 按照orders列表的顺序依次执行排序
 * - 前面的排序优先级高于后面的排序
 * - 如果前面的排序结果相同，则使用后面的排序规则
 * 
 * SQL转换：
 * - 支持将排序规则转换为SQL的ORDER BY子句
 * - 支持属性名到列名的映射（columnMap）
 * - 属性名会自动转换为下划线命名（驼峰转下划线）
 * 
 * 使用场景：
 * - 数据库查询的排序
 * - 列表数据的排序
 * - 多条件排序需求
 * 
 * 注意事项：
 * - 必须至少提供一个排序规则
 * - 排序规则按添加顺序执行
 * - 支持链式调用添加排序规则
 * - 实现Iterable接口，可以遍历所有Order
 * 
 * @since 1.0.0
 */
class Sort : Iterable<Order>, Serializable {

    private val orders: MutableList<Order>

    constructor(vararg orders: Order) : this(mutableListOf(*orders))

    constructor(orders: MutableList<Order>) {
        if (orders.isEmpty()) {
            error("必须至少提供一个排序规则！")
        }
        this.orders = orders
    }

    /**
     * 默認升序的構造器
     */
    constructor(vararg properties: String) : this(DirectionEnum.ASC, listOf<String>(*properties))

    constructor(direction: DirectionEnum, vararg properties: String) : this(direction, listOf<String>(*properties))

    constructor(direction: DirectionEnum, properties: List<String>) {
        require(properties.isNotEmpty()) { "至少提供一个排序属性！" }
        orders = ArrayList(properties.size)
        for (property in properties) {
            orders.add(Order(property, direction))
        }
    }

    fun addOrder(property: String, direction: DirectionEnum = DirectionEnum.ASC): Sort {
        orders.add(Order(property, direction))
        return this
    }

    fun and(sort: Sort?): Sort {
        if (sort == null) {
            return this
        }
        val these = ArrayList(orders)
        for (order in sort) {
            these.add(order)
        }
        return Sort(these)
    }

    fun getOrderFor(property: String): Order? {
        for (order in this) {
            if (order.property == property) {
                return order
            }
        }
        return null
    }

    override fun iterator(): Iterator<Order> {
        return orders.iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Sort) {
            return false
        }
        return orders == other.orders
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + orders.hashCode()
        return result
    }

    override fun toString(): String {
        return orders.toString()
    }


    fun getOrders(): List<Order> {
        return orders
    }

    companion object {

        private const val serialVersionUID = 5737186511679863905L

        fun add(property: String, direction: DirectionEnum): Sort {
            return Sort(Order(property, direction))
        }

        fun toSql(orders: Array<Order>, columnMap: Map<String, String>?): String {
            val orderSb = StringBuilder("ORDER BY ")
            for (order in orders) {
                val property = order.property
                val direction = order.direction.name.lowercase()
                var columnName = if (columnMap == null) {
                    property.humpToUnderscore()
                } else {
                    columnMap[property]
                }
                columnName = columnName!!.lowercase()
                orderSb.append(columnName).append(" ").append(direction).append(",")
            }
            return if (orderSb.length == 9) { // 所有指定的属性都不支持排序
                ""
            } else orderSb.substring(0, orderSb.length - 1)
        }
    }

}