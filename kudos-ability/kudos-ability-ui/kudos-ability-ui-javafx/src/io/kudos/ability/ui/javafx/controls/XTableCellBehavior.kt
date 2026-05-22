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
 * @param S 行数据类型
 * @param T 列值类型
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
     * 仅当承载本 cell 的表为 [XTableView] 时调用 [XTableView.terminateEdit]，让其它正在编辑的 cell 先提交。
     * 非 XTableView 没法做协调，静默跳过即可。
     *
     * @author K
     * @since 1.0.0
     */
    private fun tryTerminateEdit() {
//        val cell = control
//        val table = cell.tableColumn.tableView
        if (cellContainer is XTableView<*>) {
            (cellContainer as XTableView<S>).terminateEdit()
        }
    }

    /**
     * 鼠标点击事件分发；先调 [tryTerminateEdit] 让其它 cell 提交编辑，再交给 super 处理选中/激活。
     * 该方法是 jdk8_u20 从原 `simpleSelect` 拆出来的，签名也跟着变了，所以钩子改 override 这里。
     *
     * @param button 鼠标按钮
     * @param clickCount 点击次数
     * @param isAlreadySelected 该 cell 此前是否已被选中
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