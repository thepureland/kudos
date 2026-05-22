package io.kudos.ability.ui.javafx.controls.table.cell.factory

import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.util.Callback


/**
 * 复选框 [TableCell] 工厂——直接代理给 JavaFX 自带的 [CheckBoxTableCell]。
 * 单独抽工厂是为了让业务侧用 `cellFactory = CheckBoxTableCellFactory()` 的写法和本模块其它工厂统一。
 *
 * @param S 行数据类型
 * @param T 列值类型（通常为 Boolean 或 ObservableValue<Boolean>）
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CheckBoxTableCellFactory<S, T> : Callback<TableColumn<S, T>?, TableCell<S, T>?> {

    /**
     * 为列创建一个新的复选框 cell 实例。
     *
     * @param param 当前列
     * @return [CheckBoxTableCell] 实例
     * @author K
     * @since 1.0.0
     */
    override fun call(param: TableColumn<S, T>?): TableCell<S, T> {
        return CheckBoxTableCell<S, T>()
    }

}