package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Criterion测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class CriterionTest {

    @Test
    fun testConstructorWithThreeParams() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        assertEquals("name", criterion.property)
        assertEquals(OperatorEnum.EQ, criterion.operator)
        assertEquals("test", criterion.value)
        assertEquals(null, criterion.alias)
        assertFalse(criterion.encrypt)
    }

    @Test
    fun testConstructorWithFourParams() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test", "alias1")
        assertEquals("name", criterion.property)
        assertEquals(OperatorEnum.EQ, criterion.operator)
        assertEquals("test", criterion.value)
        assertEquals("alias1", criterion.alias)
        assertFalse(criterion.encrypt)
    }

    @Test
    fun testConstructorWithNullValue() {
        val criterion = Criterion("name", OperatorEnum.IS_NULL, null)
        assertEquals("name", criterion.property)
        assertEquals(OperatorEnum.IS_NULL, criterion.operator)
        assertEquals(null, criterion.value)
    }

    @Test
    fun testOperatorCodeGetter() {
        val criterion = Criterion("age", OperatorEnum.GT, 18)
        assertEquals(OperatorEnum.GT.code, criterion.operatorCode)
    }

    @Test
    fun testOperatorCodeSetter() {
        val criterion = Criterion("age", OperatorEnum.EQ, 18)
        criterion.operatorCode = OperatorEnum.LT.code
        assertEquals(OperatorEnum.LT, criterion.operator)
    }

    @Test
    fun testEncryptProperty() {
        val criterion = Criterion("password", OperatorEnum.EQ, "secret")
        assertFalse(criterion.encrypt)
        criterion.encrypt = true
        assertTrue(criterion.encrypt)
    }

    @Test
    fun testAliasProperty() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        assertEquals(null, criterion.alias)
        criterion.alias = "nameAlias"
        assertEquals("nameAlias", criterion.alias)
    }

    @Test
    fun testToString() {
        val criterion = Criterion("name", OperatorEnum.EQ, "test")
        val result = criterion.toString()
        assertTrue(result.contains("name"))
        assertTrue(result.contains(OperatorEnum.EQ.code))
        assertTrue(result.contains("test"))
    }

    @Test
    fun testToStringWithNullValue() {
        val criterion = Criterion("name", OperatorEnum.IS_NULL, null)
        val result = criterion.toString()
        assertTrue(result.contains("name"))
        assertTrue(result.contains(OperatorEnum.IS_NULL.code))
    }

    @Test
    fun testDifferentOperators() {
        val operators = listOf(
            OperatorEnum.EQ,
            OperatorEnum.NE,
            OperatorEnum.GT,
            OperatorEnum.GE,
            OperatorEnum.LT,
            OperatorEnum.LE,
            OperatorEnum.LIKE,
            OperatorEnum.IN,
            OperatorEnum.NOT_IN
        )
        
        operators.forEach { op ->
            val criterion = Criterion("field", op, "value")
            assertEquals(op, criterion.operator)
            assertEquals(op.code, criterion.operatorCode)
        }
    }

    @Test
    fun testDifferentValueTypes() {
        val stringCriterion = Criterion("name", OperatorEnum.EQ, "string")
        assertEquals("string", stringCriterion.value)

        val intCriterion = Criterion("age", OperatorEnum.EQ, 25)
        assertEquals(25, intCriterion.value)

        val doubleCriterion = Criterion("price", OperatorEnum.EQ, 99.99)
        assertEquals(99.99, doubleCriterion.value)

        val boolCriterion = Criterion("active", OperatorEnum.EQ, true)
        assertEquals(true, boolCriterion.value)

        val listCriterion = Criterion("ids", OperatorEnum.IN, listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), listCriterion.value)
    }

    @Test
    fun testPropertySetter() {
        val criterion = Criterion("oldName", OperatorEnum.EQ, "value")
        criterion.property = "newName"
        assertEquals("newName", criterion.property)
    }

    @Test
    fun testValueSetter() {
        val criterion = Criterion("name", OperatorEnum.EQ, "oldValue")
        criterion.value = "newValue"
        assertEquals("newValue", criterion.value)
    }
}
