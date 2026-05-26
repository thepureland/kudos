package io.kudos.ability.ui.javafx.controls.table.cell.factory

import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.util.Callback


/**
 * Checkbox [TableCell] factory -- simply delegates to the built-in JavaFX [CheckBoxTableCell].
 * Extracting a dedicated factory lets business code use the `cellFactory = CheckBoxTableCellFactory()`
 * style consistently with the other factories in this module.
 *
 * @param S Row data type
 * @param T Column value type (typically Boolean or ObservableValue<Boolean>)
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CheckBoxTableCellFactory<S, T> : Callback<TableColumn<S, T>?, TableCell<S, T>?> {

    /**
     * Create a new checkbox cell instance for the column.
     *
     * @param param The current column
     * @return A [CheckBoxTableCell] instance
     * @author K
     * @since 1.0.0
     */
    override fun call(param: TableColumn<S, T>?): TableCell<S, T> {
        return CheckBoxTableCell<S, T>()
    }

}