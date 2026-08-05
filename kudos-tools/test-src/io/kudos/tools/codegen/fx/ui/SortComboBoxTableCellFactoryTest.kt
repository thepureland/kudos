package io.kudos.tools.codegen.fx.ui

import io.kudos.tools.codegen.fx.FxTestSupport
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.ComboBoxTableCell
import kotlin.test.*

/**
 * test for SortComboBoxTableCellFactory
 *
 * Covers call(): every invocation returns a fresh ComboBoxTableCell pre-populated with the three sort
 * options ("", "Ascending", "Descending"), and tolerates both a real TableColumn and a null param.
 *
 * Runs on the FX application thread because instantiating ComboBoxTableCell touches the FX toolkit.
 *
 * @author K
 * @since 1.0.0
 */
internal class SortComboBoxTableCellFactoryTest {

    @Test
    fun callReturnsComboBoxCellWithSortOptions() {
        FxTestSupport.runFx {
            val factory = SortComboBoxTableCellFactory<Any>()
            val cell = factory.call(TableColumn<Any, String>())
            val comboCell = assertIs<ComboBoxTableCell<Any, String>>(cell)
            assertEquals(listOf("", "Ascending", "Descending"), comboCell.items.toList())
        }
    }

    @Test
    fun callToleratesNullParam() {
        FxTestSupport.runFx {
            val factory = SortComboBoxTableCellFactory<Any>()
            val cell = factory.call(null)
            assertNotNull(cell)
            val comboCell = assertIs<ComboBoxTableCell<Any, String>>(cell)
            assertEquals(3, comboCell.items.size)
        }
    }

    @Test
    fun callReturnsDistinctInstances() {
        FxTestSupport.runFx {
            val factory = SortComboBoxTableCellFactory<Any>()
            val c1 = factory.call(null)
            val c2 = factory.call(null)
            assertNotSame(c1, c2, "each call must build a new cell")
        }
    }
}
