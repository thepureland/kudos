package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.controls.XTextFieldTableCell
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback
import javafx.util.StringConverter

/**
 * Factory for a string-input [TableCell].
 *
 * Uses a pass-through [StringConverter] (toString / fromString are both identity); only reuses
 * the "commit when focus moves away" behavior of [XTextFieldTableCell].
 *
 * @param S Row data type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class XTextFieldTableCellFactory<S> : Callback<TableColumn<S, String>?, TableCell<S, String>?> {

    /**
     * Create a new string cell instance for the column.
     *
     * @param param The current column
     * @return An [XTextFieldTableCell] for string editing
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