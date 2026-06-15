package io.kudos.ability.ui.javafx.controls

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.control.TableColumn
import javafx.scene.control.TablePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for XTableView
 *
 * Covers: editing flag, terminateEdit no-op when not editing, terminateEdit success with a
 * cooperating listener (terminatingCell published then cleared), the IllegalStateException when
 * no cell terminates the edit, and the lazily created read-only terminatingCell property.
 *
 * @author K
 * @since 1.0.0
 */
internal class XTableViewTest {

    private fun newTable(): Pair<XTableView<String?>, TableColumn<String?, String?>> {
        val table = XTableView<String?>()
        val col = TableColumn<String?, String?>("c")
        col.setCellValueFactory { f -> ReadOnlyObjectWrapper(f.value) }
        table.columns.add(col)
        table.items.add("row0")
        table.isEditable = true
        return table to col
    }

    @Test
    fun notEditingByDefault() = runFx {
        val (table, _) = newTable()

        assertFalse(table.editing)
        assertNull(table.getTerminatingCell())
    }

    @Test
    fun terminateEditIsNoOpWhenNotEditing() = runFx {
        val (table, _) = newTable()
        var notified = false
        table.terminatingCellProperty().addListener { _, _, _ -> notified = true }

        table.terminateEdit()

        assertFalse(notified, "terminatingCell must not be touched when nothing is being edited")
    }

    @Test
    fun editStartsEditing() = runFx {
        val (table, col) = newTable()

        table.edit(0, col)

        assertTrue(table.editing)
        assertEquals(0, table.editingCell.row)
    }

    @Test
    fun terminateEditPublishesPositionAndClearsItWhenCellCooperates() = runFx {
        val (table, col) = newTable()
        val seen = mutableListOf<TablePosition<*, *>?>()
        table.terminatingCellProperty().addListener { _, _, nv ->
            seen.add(nv)
            if (nv != null) {
                // simulate a cooperating cell committing its edit
                table.edit(-1, null)
            }
        }
        table.edit(0, col)

        table.terminateEdit()

        assertFalse(table.editing)
        assertNull(table.getTerminatingCell())
        assertEquals(2, seen.size, "position must be published and then cleared")
        assertEquals(0, seen[0]!!.row)
        assertSame(col, seen[0]!!.tableColumn)
        assertNull(seen[1])
    }

    @Test
    fun terminateEditFailsWhenNoCellTerminates() = runFx {
        val (table, col) = newTable()
        table.edit(0, col)

        val e = assertFailsWith<IllegalStateException> { table.terminateEdit() }

        assertTrue(e.message!!.contains("expected editing to be terminated"))
    }

    @Test
    fun terminatingCellPropertyIsLazilyCreatedAndStable() = runFx {
        val table = XTableView<String?>()

        val p1 = table.terminatingCellProperty()
        val p2 = table.terminatingCellProperty()

        assertSame(p1, p2)
        assertEquals("terminatingCell", p1.name)
        assertSame(table, p1.bean)
        assertNull(p1.get())
        assertNull(table.getTerminatingCell())
    }
}
