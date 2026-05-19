package io.kudos.ability.ui.javafx.controls

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.ComboBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

/**
 * 给可编辑 [ComboBox] 加"输入即过滤"行为：
 *
 * - 监听 `onKeyReleased`，按当前 editor 文本（不区分大小写）做 startsWith 过滤
 * - 方向键 / Ctrl / HOME / END / TAB 跳过过滤，仅移动光标位置
 * - BACK_SPACE / DELETE 后保留光标位置，避免每次按键都跳到末尾
 *
 * **构造即注册**——`init` 块会把 ComboBox 设为 editable、装上 onKeyPressed (hide)
 * 与 onKeyReleased (本实例) 监听。所以业务侧只需：
 *
 * ```kotlin
 * val combo = ComboBox<Any>().apply { items = ... }
 * AutoCompleteComboBoxListener<String>(combo)  // 注册即生效
 * ```
 *
 * 类型参数 `T` 是 items 真实元素类型；ComboBox 自身用 `<Any>` 是历史遗留——
 * `data[i].toString()` 拿显示串，不依赖 `T`。
 */
class AutoCompleteComboBoxListener<T>(private val comboBox: ComboBox<Any>) : EventHandler<KeyEvent> {

    private val data: ObservableList<T>
    private var moveCaretToPos = false
    private var caretPos = 0

    init {
        @Suppress("UNCHECKED_CAST")
        data = comboBox.items as ObservableList<T>
        comboBox.isEditable = true
        comboBox.onKeyPressed = EventHandler { comboBox.hide() }
        comboBox.onKeyReleased = this@AutoCompleteComboBoxListener
    }

    override fun handle(event: KeyEvent) {
        when (event.code) {
            KeyCode.UP -> {
                caretPos = -1
                moveCaret(comboBox.editor.text.length)
                return
            }
            KeyCode.DOWN -> {
                if (!comboBox.isShowing) comboBox.show()
                caretPos = -1
                moveCaret(comboBox.editor.text.length)
                return
            }
            KeyCode.BACK_SPACE, KeyCode.DELETE -> {
                moveCaretToPos = true
                caretPos = comboBox.editor.caretPosition
            }
            else -> {}
        }
        if (event.code == KeyCode.RIGHT
            || event.code == KeyCode.LEFT
            || event.isControlDown
            || event.code == KeyCode.HOME
            || event.code == KeyCode.END
            || event.code == KeyCode.TAB
        ) {
            return
        }
        val list = FXCollections.observableArrayList<Any>()
        val prefix = comboBox.editor.text.lowercase()
        for (i in data.indices) {
            // .lowercase() 不带 Locale 参数走 Locale.ROOT (Kotlin 1.5+)——避免 Turkish locale 的
            // i→İ 误判，桌面 UI 输入比对依赖这一点
            if (data[i].toString().lowercase().startsWith(prefix)) {
                list.add(data[i])
            }
        }
        val t = comboBox.editor.text
        comboBox.items = list
        comboBox.editor.text = t
        if (!moveCaretToPos) caretPos = -1
        moveCaret(t.length)
        if (list.isNotEmpty()) comboBox.show()
    }

    private fun moveCaret(textLength: Int) {
        if (caretPos == -1) {
            comboBox.editor.positionCaret(textLength)
        } else {
            comboBox.editor.positionCaret(caretPos)
        }
        moveCaretToPos = false
    }

}
