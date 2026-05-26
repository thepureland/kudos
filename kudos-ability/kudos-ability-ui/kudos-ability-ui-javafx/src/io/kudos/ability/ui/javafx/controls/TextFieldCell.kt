package io.kudos.ability.ui.javafx.controls

import javafx.beans.property.Property
import javafx.scene.Node
import javafx.scene.control.Cell
import javafx.scene.control.ContentDisplay
import javafx.scene.control.TableCell
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.util.StringConverter

/**
 * A [TableCell] implementation that always displays a [TextField] (no need to click to enter
 * edit mode).
 *
 * Unlike JavaFX's built-in `TextFieldTableCell`, this class does not toggle between viewer and
 * editor states; the cell is always rendered as a [TextField], enabling a "one cell per row form"
 * continuous editing experience. Focus/hover use inline styles to change the background color,
 * avoiding visual jumps caused by mixing with the caspian.css theme.
 *
 * @param S Row data type
 * @param T Column value type
 * @param sc [StringConverter] that turns the column value into a string; when null, casts directly
 *           via `as String`
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class TextFieldCell<S, T> @JvmOverloads constructor(private val sc: StringConverter<Any>? = null) : TableCell<S, T>() {
    /** Input field that always lives inside the cell. */
    private val textField: TextField
    /** Historical: once used to bidirectionally bind an external Property; currently unused, kept for reference. */
    private val boundToCurrently: Property<T>? = null
    /**
     * JavaFX callback invoked when the row model changes: non-empty -> show the TextField and write
     * its text; empty -> fall back to plain text to avoid still rendering an input field for empty
     * cells.
     *
     * @param item Current row value
     * @param empty Whether this cell is empty (e.g. a placeholder row at the bottom of the table)
     * @author K
     * @since 1.0.0
     */
    override fun updateItem(item: T, empty: Boolean) {
        super.updateItem(item, empty)
        //        updateItem(this, sc, null, null, textField);
        if (!empty) {
            // Show the Text Field
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY)

//            // Retrieve the actual String Property that should be bound to the TextField
//            // If the TextField is currently bound to a different StringProperty
//            // Unbind the old property and rebind to the new one
//            Property<T> sp = (Property<T>) getTableColumn().getCellObservableValue(getIndex());
////            SimpleStringProperty sp = (SimpleStringProperty) ov;
//
//            if (this.boundToCurrently == null) {
//                this.boundToCurrently = sp;
//                this.textField.textProperty().bindBidirectional((Property<String>) sp);
//            } else {
//                if (this.boundToCurrently != sp) {
//                    this.textField.textProperty().unbindBidirectional(this.boundToCurrently);
//                    this.boundToCurrently = sp;
//                    this.textField.textProperty().bindBidirectional((Property<String>) this.boundToCurrently);
//                }
//            }
//            System.out.println("item=" + item + " ObservableValue<String>=" + ov.getValue());
            //this.textField.setText(item);  // No longer need this
            textField.text = if (sc == null) item as String else sc.toString(item)
        } else {
            setContentDisplay(ContentDisplay.TEXT_ONLY)
        }
    }

    companion object {
        /**
         * Generic utility to "toggle the view/editor presentation based on cell state".
         * Unlike the instance method [updateItem], this does not bind `this` and can be reused by
         * any custom cell inside its own `updateItem`.
         *
         * @param T Cell value type
         * @param cell The cell to update
         * @param converter Converter used when rendering display text; may be null
         * @param hbox Used to lay out graphic + textField horizontally when graphic is non-null
         * @param graphic Optional icon node
         * @param textField Input field used in edit mode; may be null
         * @author K
         * @since 1.0.0
         */
        fun <T> updateItem(
            cell: Cell<T?>,
            converter: StringConverter<T?>?,
            hbox: HBox,
            graphic: Node?,
            textField: TextField?
        ) {
            if (cell.isEmpty) {
                cell.text = null
                cell.setGraphic(null)
            } else {
                if (cell.isEditing) {
                    if (textField != null) {
                        textField.text =
                            getItemText(
                                cell,
                                converter
                            )
                    }
                    cell.text = null
                    if (graphic != null) {
                        hbox.children.setAll(graphic, textField)
                        cell.setGraphic(hbox)
                    } else {
                        cell.setGraphic(textField)
                    }
                } else {
                    cell.text = getItemText(
                        cell,
                        converter
                    )
                    cell.setGraphic(graphic)
                }
            }
        }

        /**
         * Extract the string representation of the cell's current value: use the converter if
         * present, otherwise [Any.toString]; returns an empty string when the cell value is null.
         *
         * @param T Cell value type
         * @param cell The cell to read from
         * @param converter String converter; may be null
         * @return The string to display
         * @author K
         * @since 1.0.0
         */
        private fun <T> getItemText(cell: Cell<T?>, converter: StringConverter<T?>?): String =
            converter?.toString(cell.item) ?: cell.item?.toString().orEmpty()

        /** Default background style: no border, white background; combined with outer cell padding 0, achieves the "input field is the cell" look. */
        private const val STYLE_DEFAULT =
            "-fx-background-color: -fx-control-inner-background;" +
                    "-fx-background-insets: 0;" +
                    "-fx-background-radius: 0;" +
                    "-fx-padding: 3 5 3 5;" +
                    "-fx-prompt-text-fill: derive(-fx-control-inner-background,-30%);" +
                    "-fx-cursor: text;"

        /** Focused state: purple outline indicating the current edit position. */
        private const val STYLE_HAS_FOCUS =
            "-fx-background-color: purple, -fx-text-box-border, -fx-control-inner-background;" +
                    "-fx-background-insets: -0.4, 1, 2;" +
                    "-fx-background-radius: 3.4, 2, 2;"

        /** Hover state: light purple background + border indicating the current hover position. */
        private const val STYLE_HOVER =
            "-fx-background-color: derive(purple,90%), -fx-text-box-border, derive(-fx-control-inner-background, 10%);" +
                    "-fx-background-insets: 1, 2.8, 3.8;" +
                    "-fx-background-radius: 3.4, 2, 2;"
    }

    init {
        // Padding in Text field cell is not wanted - we want the Textfield itself to "be"
        // the cell.  Though, this is aesthetic only.
        style = "-fx-padding: 0;"
        textField = TextField()
        // Focused and hover states should be set in the CSS.  This is just a test
        // to see what happens when we set the style in code
        textField.focusedProperty().addListener { _, _, newValue ->
            val tf = graphic as TextField
            tf.style = if (newValue) STYLE_HAS_FOCUS else STYLE_DEFAULT
        }
        textField.hoverProperty().addListener { _, _, newValue ->
            val tf = graphic as TextField
            tf.style = when {
                newValue -> STYLE_HOVER
                tf.focusedProperty().get() -> STYLE_HAS_FOCUS
                else -> STYLE_DEFAULT
            }
        }
        textField.style = STYLE_DEFAULT
        graphic = textField
    }
}