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
 * @param S 行数据类型
 * @author K
 * @since 1.0.0
 */
open class XTableView<S> : TableView<S>() {

    /**
     * 显式触发"提交并终止当前编辑"。
     * 通过把当前 editingCell 写入 [terminatingCell] 让监听该属性的 cell 自己 commitEdit，再清空回 null。
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
     * 正在终止编辑的 cell 位置。被 `edit(row, column)` 调用前置为当前编辑 cell，
     * super 之后清回 null。支持终止编辑的 TableCell 监听此属性即可在合适时机 commit。
     */
    private var terminatingCell: ReadOnlyObjectWrapper<TablePosition<S?, *>?>? = null

    /**
     * 写入 [terminatingCell]。
     * 受保护以便子类配合扩展行为复用，业务侧不直接调用。
     *
     * @param terminatingPosition 当前要终止编辑的位置；null 表示终止流程结束
     * @author K
     * @since 1.0.0
     */
    protected fun setTerminatingCell(terminatingPosition: TablePosition<S?, *>?) {
        terminatingCellPropertyImpl().set(terminatingPosition)
    }

    /**
     * 对外暴露的只读属性，cell 子类可监听以决定何时 commit。
     *
     * @return [terminatingCell] 的只读视图
     * @author K
     * @since 1.0.0
     */
    fun terminatingCellProperty(): ReadOnlyObjectProperty<TablePosition<S?, *>?> {
        return terminatingCellPropertyImpl().readOnlyProperty
    }

    /**
     * @return 当前正在终止编辑的位置；无则为 null
     * @author K
     * @since 1.0.0
     */
    fun getTerminatingCell(): TablePosition<S?, *>? {
        return terminatingCellPropertyImpl().get()
    }

    /**
     * 惰性创建 [terminatingCell] 的 [ReadOnlyObjectWrapper]。
     * 没有 cell 监听时不分配内存。
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