package io.kudos.base.query.sort

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Order测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class OrderTest {

    @Test
    fun testDefaultConstructor() {
        val order = Order()
        assertEquals("", order.property)
        assertEquals(DirectionEnum.ASC, order.direction)
    }

    @Test
    fun testConstructorWithProperty() {
        val order = Order("name")
        assertEquals("name", order.property)
        assertEquals(DirectionEnum.ASC, order.direction)
    }

    @Test
    fun testConstructorWithPropertyAndDirection() {
        val order = Order("name", DirectionEnum.DESC)
        assertEquals("name", order.property)
        assertEquals(DirectionEnum.DESC, order.direction)
    }

    @Test
    fun testIsAscending() {
        val ascOrder = Order("name", DirectionEnum.ASC)
        assertTrue(ascOrder.isAscending())

        val descOrder = Order("name", DirectionEnum.DESC)
        assertFalse(descOrder.isAscending())
    }

    @Test
    fun testToString() {
        val order = Order("name", DirectionEnum.ASC)
        val result = order.toString()
        assertTrue(result.contains("name"))
        assertTrue(result.contains("ASC"))
    }

    @Test
    fun testToStringDesc() {
        val order = Order("age", DirectionEnum.DESC)
        val result = order.toString()
        assertTrue(result.contains("age"))
        assertTrue(result.contains("DESC"))
    }

    @Test
    fun testEquals() {
        val order1 = Order("name", DirectionEnum.ASC)
        val order2 = Order("name", DirectionEnum.ASC)
        assertEquals(order1, order2)
    }

    @Test
    fun testEqualsDifferentProperty() {
        val order1 = Order("name", DirectionEnum.ASC)
        val order2 = Order("age", DirectionEnum.ASC)
        assertFalse(order1 == order2)
    }

    @Test
    fun testEqualsDifferentDirection() {
        val order1 = Order("name", DirectionEnum.ASC)
        val order2 = Order("name", DirectionEnum.DESC)
        assertFalse(order1 == order2)
    }

    @Test
    fun testEqualsSameInstance() {
        val order = Order("name", DirectionEnum.ASC)
        assertEquals(order, order)
    }

    @Test
    fun testHashCode() {
        val order1 = Order("name", DirectionEnum.ASC)
        val order2 = Order("name", DirectionEnum.ASC)
        assertEquals(order1.hashCode(), order2.hashCode())
    }

    @Test
    fun testHashCodeDifferentProperty() {
        val order1 = Order("name", DirectionEnum.ASC)
        val order2 = Order("age", DirectionEnum.ASC)
        // hashCode可能相同，但equals应该不同
        // 这里只验证equals已经测试过
        assertFalse(order1 == order2)
    }

    @Test
    fun testAscFactoryMethod() {
        val order = Order.asc("name")
        assertEquals("name", order.property)
        assertEquals(DirectionEnum.ASC, order.direction)
    }

    @Test
    fun testDescFactoryMethod() {
        val order = Order.desc("name")
        assertEquals("name", order.property)
        assertEquals(DirectionEnum.DESC, order.direction)
    }

    @Test
    fun testPropertySetter() {
        val order = Order()
        order.property = "newProperty"
        assertEquals("newProperty", order.property)
    }

    @Test
    fun testDirectionSetter() {
        val order = Order("name", DirectionEnum.ASC)
        order.direction = DirectionEnum.DESC
        assertEquals(DirectionEnum.DESC, order.direction)
        assertFalse(order.isAscending())
    }
}
