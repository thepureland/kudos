package io.kudos.ability.ui.javafx.controls

import com.sun.javafx.scene.control.behavior.TableCellBehavior
import javafx.scene.control.TableCell
import javafx.scene.input.MouseButton

/**
 * Trying to intercept the selection process to not cancel an edit.
 *
 * Issues:
 * - the control of editing is centered completely inside the cell itself
 * - there's no way to get hold of the actual editing cell, it's only the
 * TablePosition of the cell that's available
 * - on mousePressed (actually simpleSelect) this behavior is playing
 * squash: the table is the wall, the target is the editing fellow cell
 * whose edit is rudely canceled
 * - with the current api, this behavior can't do much better: table's
 * api is too weak, one method only
 * - hack here: add api to tableView that supports the notion of
 * terminate an edit and use it (note: needs cooperation of all cells
 * in the table)
 * - astonished: _why_ the need to cancel (or otherwise end an edit anywhere
 * outside this cell) - shouldn't the selection/focus update automagically
 * trigger the other cell to update its editing?
 *
 * Didn't compile in jdk8_u20 (joy of hacking ;-) :
 * - simpleSelect method signature changed
 * - edit handling at the end of former simpleSelect was extracted
 * to handleClicks (method in CellBehaviour, probably good move for
 * consistency across cell containers)
 * - so now we override the latter and try to terminate edits
 *
 *
 * @param S Row data type
 * @param T Column value type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class XTableCellBehavior<S, T>
/**
 * @param control
 */
    (control: TableCell<S, T>?) : TableCellBehavior<S, T>(control) {
    /**
     * This method is called in jdk8_u5. Signature changed
     * in jdk8_u20.
     *
     * @param e
     */
    //    @Override
    //    protected void simpleSelect(MouseEvent e) {
    //        tryTerminateEdit();
    //        super.simpleSelect(e);
    //    }
    /**
     * Only when the table hosting this cell is an [XTableView], call [XTableView.terminateEdit]
     * to let other cells currently being edited commit first. For non-XTableView there is no way
     * to coordinate, so silently skip.
     *
     * @author K
     * @since 1.0.0
     */
    private fun tryTerminateEdit() {
        (cellContainer as? XTableView<*>)?.terminateEdit()
    }

    /**
     * Mouse click event dispatch; first invoke [tryTerminateEdit] so other cells commit their edits,
     * then delegate to super for selection/activation. This method was split out from the original
     * `simpleSelect` in jdk8_u20 and its signature changed, so the hook is overridden here.
     *
     * @param button Mouse button
     * @param clickCount Click count
     * @param isAlreadySelected Whether this cell was already selected previously
     * @author K
     * @since 1.0.0
     */
    override fun handleClicks(
        button: MouseButton, clickCount: Int,
        isAlreadySelected: Boolean
    ) {
        tryTerminateEdit()
        super.handleClicks(button, clickCount, isAlreadySelected)
    }

}