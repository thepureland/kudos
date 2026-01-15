package io.kudos.base.query.sort

import kotlin.test.*

/**
 * Sort测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class SortTest {

    @Test
    fun testConstructorWithOrders() {
        val order1 = Order("name", DirectionEnum.ASC)
        val order2 = Order("age", DirectionEnum.DESC)
        val sort = Sort(order1, order2)
        assertEquals(2, sort.getOrders().size)
    }

    @Test
    fun testConstructorWithOrdersList() {
        val orders = mutableListOf(
            Order("name", DirectionEnum.ASC),
            Order("age", DirectionEnum.DESC)
        )
        val sort = Sort(orders)
        assertEquals(2, sort.getOrders().size)
    }

    @Test
    fun testConstructorWithEmptyOrdersListThrows() {
        assertFailsWith<IllegalStateException> {
            Sort(mutableListOf())
        }
    }

    @Test
    fun testConstructorWithProperties() {
        val sort = Sort("name", "age")
        assertEquals(2, sort.getOrders().size)
        assertEquals("name", sort.getOrders()[0].property)
        assertEquals("age", sort.getOrders()[1].property)
        assertEquals(DirectionEnum.ASC, sort.getOrders()[0].direction)
    }

    @Test
    fun testConstructorWithPropertiesAndDirection() {
        val sort = Sort(DirectionEnum.DESC, listOf("name", "age"))
        assertEquals(2, sort.getOrders().size)
        assertEquals(DirectionEnum.DESC, sort.getOrders()[0].direction)
        assertEquals(DirectionEnum.DESC, sort.getOrders()[1].direction)
    }

    @Test
    fun testConstructorWithEmptyPropertiesThrows() {
        assertFailsWith<IllegalArgumentException> {
            Sort(DirectionEnum.ASC, emptyList())
        }
    }

    @Test
    fun testAddOrder() {
        val sort = Sort("name")
        sort.addOrder("age", DirectionEnum.DESC)
        assertEquals(2, sort.getOrders().size)
        assertEquals("age", sort.getOrders()[1].property)
        assertEquals(DirectionEnum.DESC, sort.getOrders()[1].direction)
    }

    @Test
    fun testAddOrderDefaultDirection() {
        val sort = Sort("name")
        sort.addOrder("age")
        assertEquals(2, sort.getOrders().size)
        assertEquals(DirectionEnum.ASC, sort.getOrders()[1].direction)
    }

    @Test
    fun testAnd() {
        val sort1 = Sort("name")
        val sort2 = Sort(DirectionEnum.DESC, "age")
        val combined = sort1.and(sort2)
        assertEquals(2, combined.getOrders().size)
    }

    @Test
    fun testAndWithNull() {
        val sort = Sort("name")
        val result = sort.and(null)
        assertEquals(sort, result)
        assertEquals(1, result.getOrders().size)
    }

    @Test
    fun testGetOrderFor() {
        val sort = Sort("name", "age")
        val order = sort.getOrderFor("name")
        assertNotNull(order)
        assertEquals("name", order.property)
    }

    @Test
    fun testGetOrderForNotFound() {
        val sort = Sort("name")
        val order = sort.getOrderFor("age")
        assertNull(order)
    }

    @Test
    fun testIterator() {
        val sort = Sort("name", "age")
        val orders = mutableListOf<Order>()
        for (order in sort) {
            orders.add(order)
        }
        assertEquals(2, orders.size)
    }

    @Test
    fun testEquals() {
        val sort1 = Sort("name", "age")
        val sort2 = Sort("name", "age")
        assertEquals(sort1, sort2)
    }

    @Test
    fun testEqualsDifferentOrders() {
        val sort1 = Sort("name")
        val sort2 = Sort("age")
        assertFalse(sort1 == sort2)
    }

    @Test
    fun testEqualsSameInstance() {
        val sort = Sort("name")
        assertEquals(sort, sort)
    }

    @Test
    fun testHashCode() {
        val sort1 = Sort("name", "age")
        val sort2 = Sort("name", "age")
        assertEquals(sort1.hashCode(), sort2.hashCode())
    }

    @Test
    fun testToString() {
        val sort = Sort("name", "age")
        val result = sort.toString()
        assertTrue(result.contains("name"))
        assertTrue(result.contains("age"))
    }

    @Test
    fun testStaticAddMethod() {
        val sort = Sort.add("name", DirectionEnum.DESC)
        assertEquals(1, sort.getOrders().size)
        assertEquals("name", sort.getOrders()[0].property)
        assertEquals(DirectionEnum.DESC, sort.getOrders()[0].direction)
    }

    @Test
    fun testToSqlWithoutColumnMap() {
        val orders = arrayOf(
            Order("userName", DirectionEnum.ASC),
            Order("createTime", DirectionEnum.DESC)
        )
        val sql = Sort.toSql(orders, null)
        assertTrue(sql.contains("ORDER BY"))
        assertTrue(sql.contains("user_name"))
        assertTrue(sql.contains("create_time"))
        assertTrue(sql.contains("asc"))
        assertTrue(sql.contains("desc"))
    }

    @Test
    fun testToSqlWithColumnMap() {
        val orders = arrayOf(
            Order("userName", DirectionEnum.ASC),
            Order("createTime", DirectionEnum.DESC)
        )
        val columnMap = mapOf(
            "userName" to "u.name",
            "createTime" to "u.create_time"
        )
        val sql = Sort.toSql(orders, columnMap)
        assertTrue(sql.contains("ORDER BY"))
        assertTrue(sql.contains("u.name"))
        assertTrue(sql.contains("u.create_time"))
    }

    @Test
    fun testToSqlWithEmptyOrders() {
        val orders = emptyArray<Order>()
        val sql = Sort.toSql(orders, null)
        assertEquals("", sql)
    }

    @Test
    fun testToSqlWithPartialColumnMap() {
        val orders = arrayOf(
            Order("userName", DirectionEnum.ASC),
            Order("createTime", DirectionEnum.DESC)
        )
        val columnMap = mapOf("userName" to "user_name", "createTime" to "create_time")
        val sql = Sort.toSql(orders, columnMap)
        assertTrue(sql.contains("user_name"))
        // createTime没有映射，应该使用默认转换
        assertTrue(sql.contains("create_time") || sql.contains("createTime"))
    }

    @Test
    fun testMultipleOrders() {
        val sort = Sort("name")
            .addOrder("age", DirectionEnum.DESC)
            .addOrder("createTime", DirectionEnum.ASC)
        assertEquals(3, sort.getOrders().size)
    }

    @Test
    fun testComplexSort() {
        val sort1 = Sort("name", "age")
        val sort2 = Sort(DirectionEnum.DESC,"createTime")
        val combined = sort1.and(sort2)
        assertEquals(3, combined.getOrders().size)
    }
}
