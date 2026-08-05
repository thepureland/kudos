package io.kudos.ability.ui.javafx.controls

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.input.MouseButton
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for XTableCellBehavior
 *
 * Covers handleClicks delegation to super for plain TableViews (tryTerminateEdit silently skipped)
 * and the XTableView branch where a pending edit is terminated through a cooperating listener
 * before the click is processed.
 *
 * @author K
 * @since 1.0.0
 */
internal class XTableCellBehaviorTest {

    /** Exposes the protected handleClicks hook. */
    private class TestBehavior<S, T>(cell: TableCell<S, T>) : XTableCellBehavior<S, T>(cell) {
        fun click(button: MouseButton, count: Int, alreadySelected: Boolean) =
            handleClicks(button, count, alreadySelected)
    }

    private fun <TV : TableView<String?>> wireCell(table: TV): TableCell<String?, String?> {
        val col = TableColumn<String?, String?>("c")
        col.setCellValueFactory { f -> ReadOnlyObjectWrapper(f.value) }
        table.columns.add(col)
        table.items.add("v1")
        table.isEditable = true
        val cell = TableCell<String?, String?>()
        cell.updateTableView(table)
        cell.updateTableColumn(col)
        cell.updateIndex(0)
        return cell
    }

    @Test
    fun clickOnPlainTableViewSkipsTerminationAndDelegates() = runFx {
        val table = TableView<String?>()
        val cell = wireCell(table)
        val behavior = TestBehavior(cell)

        // primary single click on a not-yet-selected cell: no edit is started
        behavior.click(MouseButton.PRIMARY, 1, false)
        assertFalse(table.editingCell != null)

        // secondary button: handleClicks ignores it entirely
        behavior.click(MouseButton.SECONDARY, 1, false)
        assertFalse(table.editingCell != null)
    }

    @Test
    fun clickOnXTableViewWithoutEditIsNoOpTermination() = runFx {
        val table = XTableView<String?>()
        val cell = wireCell(table)
        val behavior = TestBehavior(cell)

        behavior.click(MouseButton.PRIMARY, 1, false)

        assertFalse(table.editing)
    }

    @Test
    fun clickOnXTableViewTerminatesPendingEdit() = runFx {
        val table = XTableView<String?>()
        val cell = wireCell(table)
        val behavior = TestBehavior(cell)
        // cooperating "cell": ends the edit as soon as the table publishes the terminating position
        var terminationRequested = false
        table.terminatingCellProperty().addListener { _, _, nv ->
            if (nv != null) {
                terminationRequested = true
                table.edit(-1, null)
            }
        }
        table.edit(0, table.columns[0])
        assertTrue(table.editing)

        behavior.click(MouseButton.PRIMARY, 1, false)

        assertTrue(terminationRequested, "handleClicks must first try to terminate the running edit")
        assertFalse(table.editing)
    }
}
