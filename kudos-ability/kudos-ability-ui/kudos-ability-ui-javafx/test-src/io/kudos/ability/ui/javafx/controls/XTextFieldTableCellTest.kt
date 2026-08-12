package io.kudos.ability.ui.javafx.controls

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.Node
import javafx.scene.control.TableColumn
import javafx.scene.control.TablePosition
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.util.StringConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for XTextFieldTableCell
 *
 * Covers: default and converter constructors, match (null / wrong row / wrong column / exact),
 * terminateEdit early return when not editing, startEdit without table (no-op branch) and with a
 * real editable table (textField lookup, repeated startEdit), all findTextField paths
 * (graphic-is-TextField, single .text-field lookup, match-by-text corner case, ambiguous and
 * no-match results), install/uninstall of the terminating listener when moving between plain
 * TableViews and XTableViews, and the full commit flow driven by XTableView.terminateEdit.
 *
 * Not covered: the textField focus-lost commit (needs a focused window, not deterministic) and
 * the defensive `findTextField() == null` return inside startEdit (graphic is always the
 * TextField installed by super at that point).
 *
 * @author K
 * @since 1.0.0
 */
internal class XTextFieldTableCellTest {

    /** identity converter used where commitEdit/toString must work. */
    private val converter = object : StringConverter<String?>() {
        override fun toString(obj: String?): String = obj ?: ""
        override fun fromString(string: String?): String? = string
    }

    /** Exposes protected members for direct branch testing. */
    private class TestCell<S, T>(converter: StringConverter<T?>?) : XTextFieldTableCell<S, T>(converter) {
        fun matchExposed(pos: TablePosition<S?, *>?) = match(pos)
        fun terminateEditExposed(pos: TablePosition<S?, *>?) = terminateEdit(pos)
        fun findTextFieldExposed() = findTextField()
        fun commitEditExposed() = commitEdit()
        fun addChild(node: Node) {
            children.add(node)
        }

        fun setItemState(item: T?, empty: Boolean) = updateItem(item, empty)
    }

    private fun newEditableTable(): Pair<XTableView<String?>, TableColumn<String?, String?>> {
        val table = XTableView<String?>()
        val col = TableColumn<String?, String?>("c")
        col.setCellValueFactory { f -> ReadOnlyObjectWrapper(f.value) }
        table.columns.add(col)
        table.items.add("v1")
        table.isEditable = true
        return table to col
    }

    private fun wire(
        cell: TestCell<String, String>,
        table: TableView<String?>,
        col: TableColumn<String?, String?>,
        index: Int = 0
    ) {
        cell.updateTableView(table)
        cell.updateTableColumn(col)
        cell.updateIndex(index)
    }

    @Test
    fun matchIsFalseForNullPosition() = runFx {
        val cell = TestCell<String, String>(converter)

        assertFalse(cell.matchExposed(null))
    }

    @Test
    fun matchComparesRowAndColumnIdentity() = runFx {
        val (table, col) = newEditableTable()
        val col2 = TableColumn<String?, String?>("c2")
        table.columns.add(col2)
        table.items.add("v2")
        val cell = TestCell<String, String>(converter)
        wire(cell, table, col)

        assertTrue(cell.matchExposed(TablePosition(table, 0, col)))
        assertFalse(cell.matchExposed(TablePosition(table, 1, col)), "different row must not match")
        assertFalse(cell.matchExposed(TablePosition(table, 0, col2)), "different column must not match")
    }

    @Test
    fun terminateEditReturnsWhenNotEditing() = runFx {
        val (table, col) = newEditableTable()
        val cell = TestCell<String, String>(converter)
        wire(cell, table, col)

        // must not throw although commitEdit would be illegal outside of editing
        cell.terminateEditExposed(TablePosition(table, 0, col))

        assertFalse(cell.isEditing)
    }

    @Test
    fun startEditWithoutTableIsSilentNoOp() = runFx {
        val cell = TestCell<String, String>(converter)

        cell.startEdit()

        assertFalse(cell.isEditing)
        assertNull(cell.graphic)
    }

    @Test
    fun startEditInstallsTextFieldAndSecondCallKeepsIt() = runFx {
        val (table, col) = newEditableTable()
        val cell = TestCell<String, String>(converter)
        wire(cell, table, col)
        assertEquals("v1", cell.item)

        cell.startEdit()

        assertTrue(cell.isEditing)
        val tf = cell.graphic
        assertTrue(tf is TextField)
        assertEquals("v1", tf.text)

        // repeated startEdit while editing: myTextField already set, branch skipped
        cell.startEdit()
        assertSame(tf, cell.graphic)
    }

    @Test
    fun findTextFieldReturnsGraphicWhenItIsATextField() = runFx {
        val cell = TestCell<String, String>(converter)
        val tf = TextField()
        cell.graphic = tf

        assertSame(tf, cell.findTextFieldExposed())
    }

    @Test
    fun findTextFieldReturnsSingleLookupMatch() = runFx {
        val cell = TestCell<String, String>(converter)
        val tf = TextField()
        cell.addChild(tf)

        assertSame(tf, cell.findTextFieldExposed())
    }

    @Test
    fun findTextFieldMatchesByItemTextAmongSeveralFields() = runFx {
        val cell = TestCell<String, String>(converter)
        cell.setItemState("foo", false)
        val matching = TextField("foo")
        cell.addChild(matching)
        cell.addChild(TextField("bar"))

        assertSame(matching, cell.findTextFieldExposed())
    }

    @Test
    fun findTextFieldReturnsNullWhenAmbiguous() = runFx {
        val cell = TestCell<String, String>(converter)
        cell.setItemState("foo", false)
        cell.addChild(TextField("foo"))
        cell.addChild(TextField("foo"))

        assertNull(cell.findTextFieldExposed())
    }

    @Test
    fun findTextFieldReturnsNullWhenNothingMatches() = runFx {
        val cell = TestCell<String, String>(converter)
        cell.setItemState("foo", false)
        cell.addChild(TextField("x"))
        cell.addChild(TextField("y"))

        assertNull(cell.findTextFieldExposed())
    }

    @Test
    fun terminatingListenerIsInstalledOnlyForXTableView() = runFx {
        val plain = TableView<String?>()
        val (xtable1, col1) = newEditableTable()
        val (xtable2, _) = newEditableTable()
        val cell = TestCell<String, String>(converter)

        // plain table: install skipped
        cell.updateTableView(plain)
        // plain -> XTableView: uninstall skipped, install performed
        cell.updateTableView(xtable1)
        // XTableView -> XTableView: uninstall + install
        cell.updateTableView(xtable2)
        // XTableView -> plain: uninstall only
        cell.updateTableView(plain)

        // after uninstalling, terminating notifications no longer reach the cell:
        cell.updateTableColumn(col1)
        cell.updateIndex(0)
        xtable1.edit(0, col1)
        var failed = false
        try {
            xtable1.terminateEdit()
        } catch (_: IllegalStateException) {
            failed = true // nobody listens anymore -> edit not terminated
        }
        assertTrue(failed)
    }

    @Test
    fun terminateEditViaXTableViewCommitsTheEdit() = runFx {
        val (table, col) = newEditableTable()
        val cell = TestCell<String, String>(converter)
        wire(cell, table, col)
        var committed: String? = null
        col.setOnEditCommit { committed = it.newValue }

        cell.startEdit()
        assertTrue(table.editing)
        (cell.graphic as TextField).text = "v2"

        table.terminateEdit()

        assertFalse(cell.isEditing)
        assertFalse(table.editing)
        assertEquals("v2", committed)
        assertNull(table.getTerminatingCell())
    }

    @Test
    fun defaultConstructorHasNoConverter() = runFx {
        val cell = XTextFieldTableCell<String, String>()

        assertNull(cell.converter)
    }

    @Test
    fun commitEditWithoutTextFieldUsesNullEditedValue() = runFx {
        // myTextField is still null (startEdit never ran) -> the `myTextField?.let { ... }`
        // null branch is taken and commitEdit(null) is invoked harmlessly outside editing.
        val cell = TestCell<String, String>(converter)

        cell.commitEditExposed()

        assertFalse(cell.isEditing)
    }

    @Test
    fun losingFocusCommitsTheEdit() = runFx {
        val (table, col) = newEditableTable()
        val cell = TestCell<String, String>(converter)
        wire(cell, table, col)
        var committed: String? = null
        col.setOnEditCommit { committed = it.newValue }

        // Driven through the protected setter instead of a shown Stage: a Gradle test worker is a
        // background process, so its windows never take the system focus and requestFocus() would
        // leave focusedProperty false. The focus-lost listener is what this test is about.
        cell.startEdit()
        assertTrue(cell.isEditing)
        val tf = cell.graphic as TextField
        setFocused(tf, true)
        assertTrue(tf.isFocused)
        tf.text = "edited"

        // losing focus fires the cell's focus-lost listener -> commitEdit
        setFocused(tf, false)

        assertEquals("edited", committed)
        assertFalse(cell.isEditing)
    }

    /** Invokes the internal Node.setFocused(boolean) so the focus listener fires deterministically. */
    private fun setFocused(node: Node, focused: Boolean) {
        val m = Node::class.java.getDeclaredMethod("setFocused", Boolean::class.javaPrimitiveType)
        m.isAccessible = true
        m.invoke(node, focused)
    }
}
