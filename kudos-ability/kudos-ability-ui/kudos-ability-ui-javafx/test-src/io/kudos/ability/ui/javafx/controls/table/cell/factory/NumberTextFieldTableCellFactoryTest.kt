package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import io.kudos.ability.ui.javafx.controls.XTextFieldTableCell
import javafx.scene.control.cell.TextFieldTableCell
import javafx.util.StringConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for NumberTextFieldTableCellFactory
 *
 * Covers the cell type produced by call() and the embedded integer StringConverter:
 * toString for null/values, fromString for blank, non-numeric, signed, decimal, Unicode-digit
 * and Int-overflowing input as well as regular numbers incl. zero.
 *
 * @author K
 * @since 1.0.0
 */
internal class NumberTextFieldTableCellFactoryTest {

    @Suppress("UNCHECKED_CAST")
    private fun newConverter(): StringConverter<Int?> = runFxAndGet {
        val cell = NumberTextFieldTableCellFactory<Any>().call(null)
        assertTrue(cell is XTextFieldTableCell<*, *>)
        (cell as TextFieldTableCell<Any, Int>).converter as StringConverter<Int?>
    }

    private fun <R> runFxAndGet(block: () -> R): R {
        var result: R? = null
        runFx { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    @Test
    fun toStringRendersNullAsEmptyString() {
        val converter = newConverter()

        assertEquals("", converter.toString(null))
        assertEquals("7", converter.toString(7))
        assertEquals("-3", converter.toString(-3))
    }

    @Test
    fun fromStringParsesPlainDigits() {
        val converter = newConverter()

        assertEquals(123, converter.fromString("123"))
        assertEquals(0, converter.fromString("0"))
    }

    @Test
    fun fromStringRejectsBlankInput() {
        val converter = newConverter()

        assertNull(converter.fromString(""))
        assertNull(converter.fromString("   "))
    }

    @Test
    fun fromStringRejectsNonNumericInput() {
        val converter = newConverter()

        assertNull(converter.fromString("abc"))
        assertNull(converter.fromString("12a"))
        assertNull(converter.fromString("1 2"))
        assertNull(converter.fromString("12.3"))
        // sign characters are not Unicode digits
        assertNull(converter.fromString("-5"))
        assertNull(converter.fromString("+5"))
    }

    @Test
    fun fromStringRejectsIntOverflowingDigits() {
        val converter = newConverter()

        // passes isNumeric but exceeds Int.MAX_VALUE -> toIntOrNull yields null
        assertNull(converter.fromString("99999999999"))
    }

    @Test
    fun fromStringAcceptsUnicodeDecimalDigits() {
        val converter = newConverter()

        // Arabic-Indic digits are Unicode decimal digits and parseable by toIntOrNull
        assertEquals(123, converter.fromString("١٢٣"))
        // full-width digits
        assertEquals(45, converter.fromString("４５"))
    }
}
