package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.controls.XTextFieldTableCell
import io.kudos.base.lang.string.isNumeric
import io.kudos.base.support.Consts
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback
import javafx.util.StringConverter

class NumberTextFieldTableCellFactory<S> : Callback<TableColumn<S, Int>?, TableCell<S, Int>?> {

    @Suppress(Consts.Suppress.UNCHECKED_CAST)
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