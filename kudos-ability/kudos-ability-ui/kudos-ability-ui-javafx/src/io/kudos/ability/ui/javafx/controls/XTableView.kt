package io.kudos.ability.ui.javafx.controls

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.control.TablePosition
import javafx.scene.control.TableView

/**
 * Extended TableView that supports terminating an edit.
 *
 * Implemented by a custom property terminatingCell that supporting
 * TableCells can listen to and react as appropriate.
 *
 * Collaborators:
 * - an extended TableCellBehaviour that calls tableView.terminateEdit on
 * simpleSelect before messaging super
 * - an extended TableCell that is configured with the extended TableCellBehaviour
 * and listens to the table's terminatingCell property.
 *
 * Note: all TableCells in this table need the extended behaviour.
 *
 * @param S Row data type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class XTableView<S> : TableView<S>() {

    /**
     * Explicitly trigger "commit and terminate the current edit".
     * Writes the current editingCell into [terminatingCell] so cells listening on that property
     * commit themselves, then clears it back to null.
     *
     * @author K
     * @since 1.0.0
     */
    fun terminateEdit() {
        if (!editing) {
            return
        }
        setTerminatingCell(getEditingCell())
        check(!editing) { "expected editing to be terminated but was $editingCell" }
        setTerminatingCell(null)
    }

    /**
     * Returns a boolean indicating whether this table is currently editing.
     *
     * PENDING JW: what's the exact semantics of editingCell? here we check
     * for null, what if != with row < 0 and tableColumn != null?
     *
     * @return
     */
    val editing: Boolean
        get() = editingCell != null

    /**
     * Position of the cell whose edit is being terminated. Set to the current editing cell before
     * `edit(row, column)` is invoked, then cleared back to null after super. TableCells that
     * support edit termination listen on this property to commit at the right moment.
     */
    private var terminatingCell: ReadOnlyObjectWrapper<TablePosition<S?, *>?>? = null

    /**
     * Write to [terminatingCell].
     * Protected so subclasses can reuse it together with extended behavior; not called directly
     * by business code.
     *
     * @param terminatingPosition The position whose edit is being terminated; null marks the end
     *                            of the termination flow
     * @author K
     * @since 1.0.0
     */
    protected fun setTerminatingCell(terminatingPosition: TablePosition<S?, *>?) {
        terminatingCellPropertyImpl().set(terminatingPosition)
    }

    /**
     * Read-only property exposed to the outside; cell subclasses can listen to decide when to commit.
     *
     * @return A read-only view of [terminatingCell]
     * @author K
     * @since 1.0.0
     */
    fun terminatingCellProperty(): ReadOnlyObjectProperty<TablePosition<S?, *>?> {
        return terminatingCellPropertyImpl().readOnlyProperty
    }

    /**
     * @return The position whose edit is currently being terminated; null if none
     * @author K
     * @since 1.0.0
     */
    fun getTerminatingCell(): TablePosition<S?, *>? {
        return terminatingCellPropertyImpl().get()
    }

    /**
     * Lazily create the [ReadOnlyObjectWrapper] for [terminatingCell].
     * No memory is allocated when no cell is listening.
     *
     * @author K
     * @since 1.0.0
     */
    private fun terminatingCellPropertyImpl(): ReadOnlyObjectWrapper<TablePosition<S?, *>?> {
        if (terminatingCell == null) {
            terminatingCell = ReadOnlyObjectWrapper(this, "terminatingCell")
        }
        return terminatingCell as ReadOnlyObjectWrapper<TablePosition<S?, *>?>
    }
}