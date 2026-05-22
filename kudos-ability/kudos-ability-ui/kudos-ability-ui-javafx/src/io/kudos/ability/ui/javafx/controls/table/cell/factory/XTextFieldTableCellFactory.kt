package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.controls.XTextFieldTableCell
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback
import javafx.util.StringConverter

/**
 * 字符串输入 [TableCell] 工厂。
 *
 * 使用透传式 [StringConverter]（toString / fromString 都恒等），仅复用 [XTextFieldTableCell] 的"焦点切走也提交"行为。
 *
 * @param S 行数据类型
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class XTextFieldTableCellFactory<S> : Callback<TableColumn<S, String>?, TableCell<S, String>?> {

    /**
     * 为列创建一个新的字符串 cell 实例。
     *
     * @param param 当前列
     * @return 字符串编辑用的 [XTextFieldTableCell]
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun call(param: TableColumn<S, String>?): TableCell<S, String> {
        return XTextFieldTableCell<S, String>(object :
            StringConverter<String?>() {

            override fun toString(str: String?): String? {
                return str
            }

            override fun fromString(string: String): String {
                return string
            }
        }) as TableCell<S, String>
    }

}