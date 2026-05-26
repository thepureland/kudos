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
 * Criterion test cases
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
        // Criterion is all val. To "change" a field, use copy()
        val original = Criterion("oldName", OperatorEnum.EQ, "oldValue")
        val renamed = original.copy(property = "newName")
        val revalued = original.copy(value = "newValue")
        // Original object is unchanged
        assertEquals("oldName", original.property)
        assertEquals("oldValue", original.value)
        // The copy has new values
        assertEquals("newName", renamed.property)
        assertEquals("newValue", revalued.value)
    }

    // ============================================================
    // data class auto-generated equals / hashCode / copy / componentN
    //
    // Note: the Criterion primary constructor used to have only four fields
    // (property/operator/value/alias); `encrypt` was a var outside the constructor
    // and not part of the data class generated methods.
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
        assertNotEquals(base, Criterion("y", OperatorEnum.EQ, "1", "a"), "different property")
        assertNotEquals(base, Criterion("x", OperatorEnum.NE, "1", "a"), "different operator")
        assertNotEquals(base, Criterion("x", OperatorEnum.EQ, "2", "a"), "different value")
        assertNotEquals(base, Criterion("x", OperatorEnum.EQ, "1", "b"), "different alias")
    }

    @Test
    fun testEqualsIncludesEncrypt() {
        // Historically encrypt sat outside the constructor and did not participate in equals/hashCode - fixed. Now differing encrypt -> not equal.
        val encrypted = Criterion("x", OperatorEnum.EQ, "1", encrypt = true)
        val plain = Criterion("x", OperatorEnum.EQ, "1", encrypt = false)
        assertNotEquals(encrypted, plain, "encrypt should participate in equals")
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
        // Historically encrypt sat outside the constructor and was dropped by copy - fixed. Now encrypt is preserved by copy.
        val original = Criterion("x", OperatorEnum.EQ, "1", encrypt = true)
        val copied = original.copy()
        assertTrue(copied.encrypt, "copy() should preserve encrypt")
    }

    @Test
    fun testDestructuringExposesAllFiveFields() {
        // The primary constructor now has 5 fields: property/operator/value/alias/encrypt
        val (property, operator, value, alias, encrypt) =
            Criterion("x", OperatorEnum.EQ, "1", "a", true)
        assertEquals("x", property)
        assertEquals(OperatorEnum.EQ, operator)
        assertEquals("1", value)
        assertEquals("a", alias)
        assertTrue(encrypt)
    }

    // ============================================================
    // operatorCode read-only + change operator via copy
    // ============================================================

    @Test
    fun testOperatorCodeIsReadOnlyDerivedProperty() {
        // The historical setter has been removed. Use copy to change operator.
        val original = Criterion("x", OperatorEnum.EQ, "1")
        val changed = original.copy(operator = OperatorEnum.LIKE)
        assertEquals(OperatorEnum.LIKE.code, changed.operatorCode)
        assertEquals(OperatorEnum.EQ, original.operator, "Original object unchanged")
    }

    // ============================================================
    // toString exact format
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
        // null value -> "${value ?: ""}" -> "", trailing whitespace of the whole string is trimmed
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
        // value.toString() uses the collection's default "[a, b, c]" form when concatenated into the string
        assertEquals(
            "ids IN [1, 2, 3]",
            Criterion("ids", OperatorEnum.IN, listOf(1, 2, 3)).toString()
        )
    }

    // ============================================================
    // Mutability / reference semantics
    // ============================================================

    @Test
    fun testValueIsStoredByReferenceForMutableCollections() {
        // A val field only prevents *reassignment*; the internal state of the referenced object can still be modified externally
        val list = mutableListOf(1, 2, 3)
        val criterion = Criterion("ids", OperatorEnum.IN, list)
        list.add(4)
        assertEquals(listOf(1, 2, 3, 4), criterion.value)
    }

    @Test
    fun testCriterionIsSafeAsHashMapKey() {
        // Historically the fields were var; mutating a field after placing it in a HashMap broke the hash contract - fixed.
        // Now all val, equals/hashCode are stable across the object's lifetime
        val key = Criterion("x", OperatorEnum.EQ, "1")
        val map = hashMapOf(key to "value-1")
        // Looking up with a content-equivalent new Criterion should hit
        val lookup = Criterion("x", OperatorEnum.EQ, "1")
        assertEquals("value-1", map[lookup])
    }

    // ============================================================
    // Serializable contract: JDK serialization round-trip
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

        // encrypt is now in the primary constructor -> data class equals covers it
        assertEquals(original, restored)
    }
}
