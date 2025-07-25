package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.controls.XTextFieldTableCell
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback
import javafx.util.StringConverter

class XTextFieldTableCellFactory<S> : Callback<TableColumn<S, String>?, TableCell<S, String>?> {

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