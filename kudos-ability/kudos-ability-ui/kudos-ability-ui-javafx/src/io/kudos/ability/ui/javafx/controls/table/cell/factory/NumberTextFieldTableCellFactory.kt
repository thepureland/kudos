package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.controls.XTextFieldTableCell
import io.kudos.base.lang.string.isNumeric
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback
import javafx.util.StringConverter

/**
 * 整数输入 [TableCell] 工厂。
 *
 * 用 [XTextFieldTableCell] + 整数 [StringConverter]，非数字串会被还原为 null（清空单元格），
 * 避免用户填错时把整列卡死在异常态。
 *
 * @param S 行数据类型
 * @author K
 * @since 1.0.0
 */
class NumberTextFieldTableCellFactory<S> : Callback<TableColumn<S, Int>?, TableCell<S, Int>?> {

    /**
     * 为列创建一个新的 cell 实例（JavaFX 在每行渲染时都会调）。
     *
     * @param param 当前列
     * @return 整数编辑用的 [XTextFieldTableCell]
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun call(param: TableColumn<S, Int>?): TableCell<S, Int> {
        return XTextFieldTableCell<S, Int>(object : StringConverter<Int?>() {

            override fun toString(int: Int?): String {
                return int?.toString() ?: ""
            }

            override fun fromString(string: String): Int? {
                return if (string.isBlank() || !string.isNumeric()) null else string.toInt()
            }
        }) as TableCell<S, Int>
    }

}