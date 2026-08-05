package io.kudos.ability.ui.javafx.controls

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.TablePosition
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.cell.TextFieldTableCell
import javafx.util.StringConverter

/**
 * TexFieldTableCell which supports terminating an ongoing edit.
 *
 *
 *
 * There are two parts of the support:
 * - listens to the textField's focusProperty and commits on loosing. This
 * handles the case when the focus is moved to something outside the table
 * - installs a custom skin with a behaviour that tries to terminate the
 * edit (vs. cancelling it)
 * - listens to the table's terminatingCell property and commits if the new
 * property matches this cell's position (requires the table to be
 * of type XTableView)
 *
 * @param S row data type
 * @param T column value type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class XTextFieldTableCell<S, T> @JvmOverloads constructor(converter: StringConverter<T?>? = null) :
    TextFieldTableCell<S?, T?>(converter) {
    /** local copy of the textfield that's installed by super.  */
    private var myTextField: TextField? = null

    /** the changeListener for the table's terminatingCell property  */
    private val terminatingListener =
        ChangeListener { _: ObservableValue<out TablePosition<S?, *>?>?, _: TablePosition<S?, *>?,
                         newPosition: TablePosition<S?, *>? ->
            terminateEdit(
                newPosition
            )
        }

    /**
     * {@inheritDoc}
     *
     * In addition to super: looks up the [TextField] installed by super ([findTextField]) and
     * listens for its focus-lost event so that losing focus triggers commitEdit directly — this
     * lets "clicking elsewhere" also trigger a commit, not only the Enter key.
     *
     * TBD: cleanup, probably needs WeakListener
     *
     * @author K
     * @since 1.0.0
     */
    override fun startEdit() {
        super.startEdit()
        if (isEditing && myTextField == null) {
            // something unexpected happened when the lookup fails ... return silently
            val tf = findTextField() ?: return
            myTextField = tf
            tf.focusedProperty().addListener { _, _, nvalue ->
                if (!nvalue) {
                    commitEdit(converter.fromString(tf.text))
                }
            }
        }
    }

    /**
     * Listener callback for [XTableView.terminatingCellProperty]: commits the edit when the new
     * position matches this cell.
     *
     * @param newPosition new terminating position on the table
     * @author K
     * @since 1.0.0
     */
    protected fun terminateEdit(newPosition: TablePosition<S?, *>?) {
        if (!isEditing || !match(newPosition)) return
        commitEdit()
    }

    /**
     * Converts the current [myTextField] text back to T via the converter and then commitEdit.
     * super's commitEdit requires a T argument; this wrapper avoids repeating the converter
     * call at each call site.
     *
     * @author K
     * @since 1.0.0
     */
    protected fun commitEdit() {
        val edited: T? = myTextField?.let { converter.fromString(it.text) }
        commitEdit(edited)
    }

    /**
     * Returns whether the given [TablePosition] is exactly this cell's position.
     * The superclass marks the same method private ("WTF is that method private?"), so we have
     * to copy-paste a version here.
     *
     * @param pos a TablePosition to check for matching
     * @return true if the given position matches this cell, false otherwise.
     * @author K
     * @since 1.0.0
     */
    protected fun match(pos: TablePosition<S?, *>?): Boolean {
        return pos != null && pos.row == index && pos.tableColumn === tableColumn
    }

    /**
     * Looks up the inner [TextField] installed by super.
     *
     * Three match paths:
     * 1. cell.graphic is the TextField (no decoration scenario);
     * 2. the `.text-field` selector matches exactly one node;
     * 3. graphic is a container with multiple TextFields, matched by the current item's string
     *    value.
     *
     * The third is a corner case; the source comment marks it as "TBD: untested!" — do not
     * rely on it.
     *
     * @return the [TextField] found; null when none match
     * @author K
     * @since 1.0.0
     */
    protected fun findTextField(): TextField? {
        if (graphic is TextField) {
            // no "real" graphic so the textfield _is_ the graphic
            return graphic as TextField
        }
        // TBD: untested!
        // has a "real" graphic, the graphic property is a pane which
        // contains the textfield
        val nodes = lookupAll(".text-field")
        // sane use-case: it's only one field
        if (nodes.size == 1) {
            return nodes.first() as TextField
        }
        // corner case: there's a "real" graphic which is/contains
        // a textfield, differentiate by text
        val expectedText = converter.toString(item)
        val fields = nodes.filterIsInstance<TextField>().filter { it.text == expectedText }
        return fields.singleOrNull()
    }

    /**
     * @param newTable
     */
    private fun installTerminatingListener(newTable: TableView<S?>) {
        if (newTable is XTableView<*>) {
            (newTable as XTableView<S?>).terminatingCellProperty().addListener(terminatingListener)
        }
    }

    /**
     * @param oldTable
     */
    @Suppress("UNCHECKED_CAST")
    private fun uninstallTerminatingListener(oldTable: TableView<S?>?) {
        if (oldTable is XTableView<*>) {
            (oldTable.terminatingCellProperty() as ObservableValue<*>).removeListener(terminatingListener as ChangeListener<in Any>)
        }
    }

    /**
     * Implemented to listen to the tableViewProperty and un-/wire the terminatingListener
     * as appropriate.
     *
     * @param converter
     */
    init {
        tableViewProperty().addListener { _: ObservableValue<out TableView<S?>>?, oldTable: TableView<S?>?, newTable: TableView<S?> ->
            uninstallTerminatingListener(oldTable)
            installTerminatingListener(newTable)
        }
    }
}