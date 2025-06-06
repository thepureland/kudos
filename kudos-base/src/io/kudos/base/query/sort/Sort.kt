package io.kudos.base.query.sort

import io.kudos.base.lang.string.humpToUnderscore
import java.io.Serializable

/**
 * 排序规则
 *
 * @author K
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

    constructor(vararg properties: String, direction: DirectionEnum = DirectionEnum.ASC) : this(direction, listOf<String>(*properties))

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

        fun toSql(orders: Array<Order>, columnMap: Map<String?, String?>?): String {
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