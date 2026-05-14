package io.kudos.base.query

import io.kudos.base.query.enums.OperatorEnum
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
    fun testEncryptDefaultsToFalse() {
        val criterion = Criterion("password", OperatorEnum.EQ, "secret")
        assertFalse(criterion.encrypt)
    }

    @Test
    fun testEncryptCanBeSetViaConstructor() {
        val criterion = Criterion("password", OperatorEnum.EQ, "secret", encrypt = true)
        assertTrue(criterion.encrypt)
    }

    @Test
    fun testAliasDefaultsToNullAndCanBeSetViaConstructor() {
        val noAlias = Criterion("name", OperatorEnum.EQ, "test")
        assertEquals(null, noAlias.alias)
        val withAlias = Criterion("name", OperatorEnum.EQ, "test", alias = "nameAlias")
        assertEquals("nameAlias", withAlias.alias)
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
    fun testFieldsAreImmutable_useCopyToCreateModifiedVersion() {
        // Criterion 已经全 val。要"改"字段就 copy()
        val original = Criterion("oldName", OperatorEnum.EQ, "oldValue")
        val renamed = original.copy(property = "newName")
        val revalued = original.copy(value = "newValue")
        // 原对象不变
        assertEquals("oldName", original.property)
        assertEquals("oldValue", original.value)
        // copy 出来的有新值
        assertEquals("newName", renamed.property)
        assertEquals("newValue", revalued.value)
    }

    // ============================================================
    // data class 自动生成的 equals / hashCode / copy / componentN
    //
    // 注意 Criterion 主构造器只有 property/operator/value/alias 四个字段，
    // `encrypt` 是构造体外的 var，不在 data class 生成方法里
    // ============================================================

    @Test
    fun testEqualsAndHashCodeBasedOnFourPrimaryFields() {
        val a = Criterion("x", OperatorEnum.EQ, "1", "alias-a")
        val b = Criterion("x", OperatorEnum.EQ, "1", "alias-a")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testEqualsDistinguishesEachPrimaryField() {
        val base = Criterion("x", OperatorEnum.EQ, "1", "a")
        assertNotEquals(base, Criterion("y", OperatorEnum.EQ, "1", "a"), "property 不同")
        assertNotEquals(base, Criterion("x", OperatorEnum.NE, "1", "a"), "operator 不同")
        assertNotEquals(base, Criterion("x", OperatorEnum.EQ, "2", "a"), "value 不同")
        assertNotEquals(base, Criterion("x", OperatorEnum.EQ, "1", "b"), "alias 不同")
    }

    @Test
    fun testEqualsIncludesEncrypt() {
        // 历史上 encrypt 在构造器外，不参与 equals/hashCode——已修。现在 encrypt 不同 → not equal
        val encrypted = Criterion("x", OperatorEnum.EQ, "1", encrypt = true)
        val plain = Criterion("x", OperatorEnum.EQ, "1", encrypt = false)
        assertNotEquals(encrypted, plain, "encrypt 应参与 equals")
        assertNotEquals(encrypted.hashCode(), plain.hashCode())
    }

    @Test
    fun testCopyAllowsPartialChange() {
        val original = Criterion("x", OperatorEnum.EQ, "1", "a")
        val copied = original.copy(value = "2")
        assertEquals("x", copied.property)
        assertEquals(OperatorEnum.EQ, copied.operator)
        assertEquals("2", copied.value)
        assertEquals("a", copied.alias)
    }

    @Test
    fun testCopyCarriesEncrypt() {
        // 历史上 encrypt 在构造器外被 copy 丢弃——已修。现在 encrypt 跟随 copy
        val original = Criterion("x", OperatorEnum.EQ, "1", encrypt = true)
        val copied = original.copy()
        assertTrue(copied.encrypt, "copy() 应保留 encrypt")
    }

    @Test
    fun testDestructuringExposesAllFiveFields() {
        // 现在主构造器有 5 个字段：property/operator/value/alias/encrypt
        val (property, operator, value, alias, encrypt) =
            Criterion("x", OperatorEnum.EQ, "1", "a", true)
        assertEquals("x", property)
        assertEquals(OperatorEnum.EQ, operator)
        assertEquals("1", value)
        assertEquals("a", alias)
        assertTrue(encrypt)
    }

    // ============================================================
    // operatorCode 只读 + copy 改 operator
    // ============================================================

    @Test
    fun testOperatorCodeIsReadOnlyDerivedProperty() {
        // 历史上的 setter 已移除。要改 operator 请用 copy
        val original = Criterion("x", OperatorEnum.EQ, "1")
        val changed = original.copy(operator = OperatorEnum.LIKE)
        assertEquals(OperatorEnum.LIKE.code, changed.operatorCode)
        assertEquals(OperatorEnum.EQ, original.operator, "原对象不变")
    }

    // ============================================================
    // toString 精确格式
    // ============================================================

    @Test
    fun testToStringExactFormat() {
        assertEquals(
            "name = alice",
            Criterion("name", OperatorEnum.EQ, "alice").toString()
        )
    }

    @Test
    fun testToStringTrimsTrailingSpaceWhenValueIsNull() {
        // null value → "${value ?: ""}" → ""，整串末尾空格被 trim
        assertEquals(
            "x IS NULL",
            Criterion("x", OperatorEnum.IS_NULL, null).toString()
        )
    }

    @Test
    fun testToStringRendersNumericValue() {
        assertEquals(
            "age > 18",
            Criterion("age", OperatorEnum.GT, 18).toString()
        )
    }

    @Test
    fun testToStringRendersCollectionViaToString() {
        // value.toString() 用集合默认的 "[a, b, c]" 形式拼到字符串里
        assertEquals(
            "ids IN [1, 2, 3]",
            Criterion("ids", OperatorEnum.IN, listOf(1, 2, 3)).toString()
        )
    }

    // ============================================================
    // 可变性 / 引用语义
    // ============================================================

    @Test
    fun testValueIsStoredByReferenceForMutableCollections() {
        // val 字段只阻止 *重新赋值*，被引用对象的内部状态仍可外部修改
        val list = mutableListOf(1, 2, 3)
        val criterion = Criterion("ids", OperatorEnum.IN, list)
        list.add(4)
        assertEquals(listOf(1, 2, 3, 4), criterion.value)
    }

    @Test
    fun testCriterionIsSafeAsHashMapKey() {
        // 历史上字段是 var，放进 HashMap 后改字段会破坏哈希契约——已修
        // 现在全 val，equals/hashCode 在对象生命周期内稳定
        val key = Criterion("x", OperatorEnum.EQ, "1")
        val map = hashMapOf(key to "value-1")
        // 用一个内容等价的新 Criterion 查找，应能命中
        val lookup = Criterion("x", OperatorEnum.EQ, "1")
        assertEquals("value-1", map[lookup])
    }

    // ============================================================
    // Serializable 契约：JDK 序列化往返
    // ============================================================

    @Test
    fun testJdkSerializationRoundTrip() {
        val original = Criterion("x", OperatorEnum.EQ, "1", "alias-a", encrypt = true)
        val bytes = ByteArrayOutputStream().also { baos ->
            ObjectOutputStream(baos).use { it.writeObject(original) }
        }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as Criterion
        }

        // encrypt 已在主构造器内 → data class equals 直接覆盖
        assertEquals(original, restored)
    }
}
