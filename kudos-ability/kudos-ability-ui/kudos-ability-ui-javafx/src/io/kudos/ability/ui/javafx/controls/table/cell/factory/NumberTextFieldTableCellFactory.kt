package io.kudos.ability.ui.javafx.controls.table.cell.factory

import io.kudos.ability.ui.javafx.controls.XTextFieldTableCell
import io.kudos.base.lang.string.isNumeric
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback
import javafx.util.StringConverter

/**
 * Factory for an integer-input [TableCell].
 *
 * Uses [XTextFieldTableCell] + an integer [StringConverter]; non-numeric strings are restored to
 * null (clearing the cell) to avoid stalling the whole column in an error state when the user
 * mistypes.
 *
 * @param S Row data type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class NumberTextFieldTableCellFactory<S> : Callback<TableColumn<S, Int>?, TableCell<S, Int>?> {

    /**
     * Create a new cell instance for the column (JavaFX invokes this when rendering each row).
     *
     * @param param The current column
     * @return An [XTextFieldTableCell] for integer editing
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