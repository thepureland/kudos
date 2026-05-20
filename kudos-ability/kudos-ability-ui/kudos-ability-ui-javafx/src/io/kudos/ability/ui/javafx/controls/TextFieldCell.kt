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
 * 一直显示 [TextField] 的 [TableCell] 实现（无需点击进入编辑态）。
 *
 * 与 JavaFX 自带 `TextFieldTableCell` 的区别是本类不进入"viewer 态/editor 态"的切换，
 * 单元格始终渲染为 [TextField]，便于"一格一行表单"的连续编辑体验。
 * focus/hover 用内联 style 改变背景色，避免和 caspian.css 主题混入造成的视觉跳变。
 *
 * @param S 行数据类型
 * @param T 列值类型
 * @param sc 把列值转字符串的 [StringConverter]；为 null 时直接 `as String`
 * @author K
 * @since 1.0.0
 */
class TextFieldCell<S, T> @JvmOverloads constructor(private val sc: StringConverter<Any>? = null) : TableCell<S, T>() {
    /** 单元格内常驻的输入框 */
    private val textField: TextField
    /** 历史保留：曾用于双向绑定外部 Property，目前由模板代码注释掉而未使用 */
    private val boundToCurrently: Property<T>? = null
    /**
     * JavaFX 行模型变化时回调：非空 → 显示 TextField 并写入文本；空 → 退回纯文本展示，避免空单元格仍渲染输入框。
     *
     * @param item 当前行值
     * @param empty 该 cell 是否为空（如表格底部的占位行）
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
         * 通用的"按 cell 状态切换 view/editor 表现"工具方法。
         * 与实例方法 [updateItem] 不同，本方法不绑定 `this`，可被任意自定义 Cell 在 `updateItem` 中复用。
         *
         * @param T cell 值类型
         * @param cell 待更新的 cell
         * @param converter 显示文本时使用的转换器，可为 null
         * @param hbox 当 graphic 非 null 时用于水平摆放 graphic+textField
         * @param graphic 可选的图标节点
         * @param textField 编辑态使用的输入框，可为 null
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
         * 提取 cell 当前值的字符串表示：有 converter 走 converter，否则用 [Any.toString]，cell 值为 null 返回空串。
         *
         * @param T cell 值类型
         * @param cell 待取值的 cell
         * @param converter 字符串转换器，可为 null
         * @return 用于显示的字符串
         * @author K
         * @since 1.0.0
         */
        private fun <T> getItemText(cell: Cell<T?>, converter: StringConverter<T?>?): String {
            return if (converter == null) if (cell.item == null) "" else cell.item.toString() else converter.toString(
                cell.item
            )
        }
    }

    init {
        var strCss: String
        // Padding in Text field cell is not wanted - we want the Textfield itself to "be"
        // The cell.  Though, this is aesthetic only.  to each his own.  comment out
        // to revert back.
        strCss = "-fx-padding: 0;"
        style = strCss
        textField = TextField()

        //
        // Default style pulled from caspian.css. Used to play around with the inset background colors
        // ---trying to produce a text box without borders
        strCss =
            "" +  //"-fx-background-color: -fx-shadow-highlight-color, -fx-text-box-border, -fx-control-inner-background;" +
                    "-fx-background-color: -fx-control-inner-background;" +  //"-fx-background-insets: 0, 1, 2;" +
                    "-fx-background-insets: 0;" +  //"-fx-background-radius: 3, 2, 2;" +
                    "-fx-background-radius: 0;" +
                    "-fx-padding: 3 5 3 5;" +  /*Play with this value to center the text depending on cell height??*/ //"-fx-padding: 0 0 0 0;" +
                    "-fx-prompt-text-fill: derive(-fx-control-inner-background,-30%);" +
                    "-fx-cursor: text;" +
                    ""
        // Focused and hover states should be set in the CSS.  This is just a test
        // to see what happens when we set the style in code
        textField.focusedProperty().addListener { _, _, newValue ->
            val tf = graphic as TextField
            val strStyleGotFocus = "-fx-background-color: purple, -fx-text-box-border, -fx-control-inner-background;" +
                    "-fx-background-insets: -0.4, 1, 2;" +
                    "-fx-background-radius: 3.4, 2, 2;"
            val strStyleLostFocus =
                //"-fx-background-color: -fx-shadow-highlight-color, -fx-text-box-border, -fx-control-inner-background;" +
                "-fx-background-color: -fx-control-inner-background;" +  //"-fx-background-insets: 0, 1, 2;" +
                        "-fx-background-insets: 0;" +  //"-fx-background-radius: 3, 2, 2;" +
                        "-fx-background-radius: 0;" +
                        "-fx-padding: 3 5 3 5;" +  /**/ //"-fx-padding: 0 0 0 0;" +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background,-30%);" +
                        "-fx-cursor: text;" +
                        ""
            if (newValue) {
                tf.style = strStyleGotFocus
            } else {
                tf.style = strStyleLostFocus
            }
        }
        textField.hoverProperty().addListener { _, _, newValue ->
            val tf = graphic as TextField
            val strStyleGotHover =
                "-fx-background-color: derive(purple,90%), -fx-text-box-border, derive(-fx-control-inner-background, 10%);" +
                        "-fx-background-insets: 1, 2.8, 3.8;" +
                        "-fx-background-radius: 3.4, 2, 2;"
            val strStyleLostHover =
                //"-fx-background-color: -fx-shadow-highlight-color, -fx-text-box-border, -fx-control-inner-background;" +
                "-fx-background-color: -fx-control-inner-background;" +  //"-fx-background-insets: 0, 1, 2;" +
                        "-fx-background-insets: 0;" +  //"-fx-background-radius: 3, 2, 2;" +
                        "-fx-background-radius: 0;" +
                        "-fx-padding: 3 5 3 5;" +  /**/ //"-fx-padding: 0 0 0 0;" +
                        "-fx-prompt-text-fill: derive(-fx-control-inner-background,-30%);" +
                        "-fx-cursor: text;" +
                        ""
            val strStyleHasFocus = "-fx-background-color: purple, -fx-text-box-border, -fx-control-inner-background;" +
                    "-fx-background-insets: -0.4, 1, 2;" +
                    "-fx-background-radius: 3.4, 2, 2;"
            if (newValue) {
                tf.style = strStyleGotHover
            } else {
                if (!tf.focusedProperty().get()) {
                    tf.style = strStyleLostHover
                } else {
                    tf.style = strStyleHasFocus
                }
            }
        }
        textField.style = strCss
        graphic = textField
    }
}