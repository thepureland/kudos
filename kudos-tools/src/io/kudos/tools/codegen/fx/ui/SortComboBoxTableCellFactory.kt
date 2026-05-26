package io.kudos.tools.codegen.fx.ui

import javafx.collections.FXCollections
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.util.Callback

/**
 * Sort-order ComboBox table cell factory.
 *
 * @author K
 * @since 1.0.0
 */
class SortComboBoxTableCellFactory<S>: Callback<TableColumn<S, String>?, TableCell<S, String>?> {

    @Suppress("UNCHECKED_CAST")
    override fun call(param: TableColumn<S, String>?): TableCell<S, String> {
        val strings = FXCollections.observableArrayList("", "Ascending", "Descending")
        return ComboBoxTableCell<S?, Any?>(*strings.toTypedArray()) as TableCell<S, String>
    }

}