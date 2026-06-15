package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import io.kudos.ability.ui.javafx.controls.XTextFieldTableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.TextFieldTableCell
import javafx.util.StringConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for XTextFieldTableCellFactory
 *
 * Covers the produced cell type and the pass-through StringConverter (identity in both
 * directions including null and Unicode strings).
 *
 * @author K
 * @since 1.0.0
 */
internal class XTextFieldTableCellFactoryTest {

    @Test
    fun createsXTextFieldTableCellWithPassThroughConverter() = runFx {
        val factory = XTextFieldTableCellFactory<Any>()

        val cell = factory.call(TableColumn("c"))

        assertTrue(cell is XTextFieldTableCell<*, *>)
        @Suppress("UNCHECKED_CAST")
        val converter = (cell as TextFieldTableCell<Any, String>).converter as StringConverter<String?>
        assertNull(converter.toString(null))
        assertEquals("abc", converter.toString("abc"))
        assertEquals("中文©", converter.toString("中文©"))
        assertEquals("", converter.fromString(""))
        assertEquals("xyz", converter.fromString("xyz"))
    }

    @Test
    fun everyCallCreatesAFreshCellEvenForNullColumn() = runFx {
        val factory = XTextFieldTableCellFactory<Any>()

        val c1 = factory.call(null)
        val c2 = factory.call(null)

        assertTrue(c1 is XTextFieldTableCell<*, *>)
        assertNotSame(c1, c2)
    }
}
