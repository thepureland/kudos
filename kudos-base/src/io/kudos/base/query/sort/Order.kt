package io.kudos.base.query.sort

import java.io.Serializable

/**
 * Wrapper class for a single sort rule.
 *
 * Encapsulates the sort rule for one property, including the property name and sort direction.
 *
 * Core properties:
 * - property: name of the property (field) to sort by
 * - direction: sort direction (ASC ascending, DESC descending)
 *
 * Sort direction:
 * - ASC: ascending order, from small to large
 * - DESC: descending order, from large to small
 *
 * Use cases:
 * - Sorting in database queries
 * - Sorting of list data
 * - Used together with Sort class to implement multi-property sorting
 *
 * Notes:
 * - The property name is used to generate the SQL ORDER BY clause
 * - Supports chained calls and static factory methods
 * - Implements Serializable for serialization support
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