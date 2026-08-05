package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.CheckBoxTableCell
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * test for CheckBoxTableCellFactory
 *
 * Covers cell creation for null and non-null columns and that every call yields a fresh
 * CheckBoxTableCell instance.
 *
 * @author K
 * @since 1.0.0
 */
internal class CheckBoxTableCellFactoryTest {

    @Test
    fun createsCheckBoxTableCellEvenForNullColumn() = runFx {
        val factory = CheckBoxTableCellFactory<Any, Boolean>()

        val cell = factory.call(null)

        assertTrue(cell is CheckBoxTableCell)
    }

    @Test
    fun everyCallCreatesAFreshCell() = runFx {
        val factory = CheckBoxTableCellFactory<Any, Boolean>()
        val column = TableColumn<Any, Boolean>("c")

        val c1 = factory.call(column)
        val c2 = factory.call(column)

        assertTrue(c1 is CheckBoxTableCell)
        assertNotSame(c1, c2)
    }
}
