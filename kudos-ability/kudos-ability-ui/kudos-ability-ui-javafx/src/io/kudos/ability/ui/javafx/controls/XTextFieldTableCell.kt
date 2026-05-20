package io.kudos.ability.ui.javafx.controls

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.TablePosition
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
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
 * @param S 行数据类型
 * @param T 列值类型
 * @author K
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
     * 覆盖 super 之外额外做的事：把 super 安装好的 [TextField] 反查出来（[findTextField]），
     * 监听其焦点丢失事件，离开焦点时直接 commitEdit——这是为了让"点别处"也能触发提交，
     * 而不是仅靠 Enter 键。
     *
     * TBD: cleanup, probably needs WeakListener
     *
     * @author K
     * @since 1.0.0
     */
    override fun startEdit() {
        super.startEdit()
        if (isEditing && myTextField == null) {
            myTextField = findTextField()
            if (myTextField == null) {
                // something unexpected happened ...
                // either throw an exception or return silently
                return
            }
            val tf = requireNotNull(myTextField) { "myTextField is null" }
            tf.focusedProperty().addListener { _, _, nvalue ->
                if (!nvalue) {
                    commitEdit(converter.fromString(tf.text))
                }
            }
        }
    }

    /**
     * 监听 [XTableView.terminatingCellProperty] 的回调：当新位置正好匹配本 cell 时提交编辑。
     *
     * @param newPosition 表上新的终止位置
     * @author K
     * @since 1.0.0
     */
    protected fun terminateEdit(newPosition: TablePosition<S?, *>?) {
        if (!isEditing || !match(newPosition)) return
        commitEdit()
    }

    /**
     * 把当前 [myTextField] 的文本通过 converter 转回 T 再 commitEdit。
     * super 的 commitEdit 默认需要传 T，这里封装一层，避免每个调用点都重复 converter 调用。
     *
     * @author K
     * @since 1.0.0
     */
    protected fun commitEdit() {
        val edited: T? = myTextField?.let { converter.fromString(it.text) }
        commitEdit(edited)
    }

    /**
     * 判断给定 [TablePosition] 是否就是本 cell 所在的位置。
     * super 类把同名方法标成了 private（"WTF is that method private?"），不得不在此 c&p 一份。
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
     * 反查 super 已安装的内部 [TextField]。
     *
     * 三种命中路径：
     * 1. cell.graphic 就是 TextField（无装饰场景）；
     * 2. `.text-field` 选择器只命中一个节点；
     * 3. graphic 是含多个 TextField 的容器，按当前 item 的字符串值匹配。
     *
     * 第三种是边角情况，源代码注释标了 "TBD: untested!"，慎依赖。
     *
     * @return 找到的 [TextField]；都未命中返回 null
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
            return nodes.toTypedArray()[0] as TextField
        }
        // corner case: there's a "real" graphic which is/contains
        // a textfield, differentiate by text
        val expectedText = converter.toString(item)
        val fields = nodes
            .filter { it is TextField && expectedText == (it as TextInputControl).text }
            .toList()
        return if (fields.size == 1) fields[0] as TextField else null
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