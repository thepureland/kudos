package io.kudos.base.query.sort

import io.kudos.base.lang.string.humpToUnderscore
import java.io.Serializable

/**
 * Sort rule wrapper class.
 *
 * Encapsulates multiple sort rules, supporting multi-property sorting and combinations of sort directions.
 *
 * Core features:
 * 1. Multi-property sorting: supports sorting by multiple properties
 * 2. Sort direction: each property can specify an independent sort direction (ASC/DESC)
 * 3. Sort merging: supports merging multiple Sort objects via the `and` method
 * 4. SQL generation: supports converting sort rules into a SQL ORDER BY clause
 *
 * Data structure:
 * - orders: list of sort rules, executed in insertion order
 * - Each Order contains a property name and a sort direction
 *
 * Sort execution order:
 * - Sorts are applied in the order they appear in the orders list
 * - Earlier sorts have higher priority than later ones
 * - If earlier sort comparisons are equal, later sort rules are used as tiebreakers
 *
 * SQL conversion:
 * - Supports converting sort rules into a SQL ORDER BY clause
 * - Supports mapping from property names to column names (columnMap)
 * - Property names are automatically converted from camelCase to snake_case
 *
 * Use cases:
 * - Sorting in database queries
 * - Sorting of list data
 * - Multi-criteria sort requirements
 *
 * Notes:
 * - At least one sort rule must be provided
 * - Sort rules are executed in insertion order
 * - Supports chained calls to add sort rules
 * - Implements Iterable, so all Orders can be iterated
 *
 * @since 1.0.0
 */
class Sort : Iterable<Order>, Serializable {

    private val orders: MutableList<Order>

    constructor(vararg orders: Order) : this(mutableListOf(*orders))

    constructor(orders: MutableList<Order>) {
        if (orders.isEmpty()) {
            error("At least one sort rule must be provided!")
        }
        this.orders = orders
    }

    /**
     * Constructor that defaults to ascending order.
     */
    constructor(vararg properties: String) : this(DirectionEnum.ASC, properties.toList())

    constructor(direction: DirectionEnum, vararg properties: String) : this(direction, properties.toList())

    constructor(direction: DirectionEnum, properties: List<String>) {
        require(properties.isNotEmpty()) { "At least one sort property must be provided!" }
        orders = properties.mapTo(ArrayList(properties.size)) { Order(it, direction) }
    }

    fun addOrder(property: String, direction: DirectionEnum = DirectionEnum.ASC): Sort {
        orders.add(Order(property, direction))
        return this
    }

    fun and(sort: Sort?): Sort {
        if (sort == null) {
            return this
        }
        return Sort((orders + sort.getOrders()).toMutableList())
    }

    fun getOrderFor(property: String): Order? = orders.firstOrNull { it.property == property }

    override fun iterator(): Iterator<Order> = orders.iterator()

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
        return 31 * 17 + orders.hashCode()
    }

    override fun toString(): String {
        return orders.toString()
    }


    fun getOrders(): List<Order> {
        return orders
    }

    companion object {

        private const val serialVersionUID = 5737186511679863905L

        fun add(property: String, direction: DirectionEnum): Sort = Sort(Order(property, direction))

        fun toSql(orders: Array<Order>, columnMap: Map<String, String>?): String {
            if (orders.isEmpty()) return ""
            return orders.joinToString(separator = ",", prefix = "ORDER BY ") { order ->
                val direction = order.direction.name.lowercase()
                val columnName = (columnMap?.get(order.property) ?: order.property.humpToUnderscore()).lowercase()
                "$columnName $direction"
            }
        }
    }

}