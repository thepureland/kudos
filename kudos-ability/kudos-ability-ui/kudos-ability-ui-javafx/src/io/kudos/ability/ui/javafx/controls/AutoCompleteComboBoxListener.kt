package io.kudos.ability.ui.javafx.controls

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.ComboBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

class AutoCompleteComboBoxListener<T>(private val comboBox: ComboBox<Any>?) : EventHandler<KeyEvent> {
    private val sb: StringBuilder
    private val data: ObservableList<T>
    private var moveCaretToPos = false
    private var caretPos = 0
    override fun handle(event: KeyEvent) {
        if (event.code == KeyCode.UP) {
            caretPos = -1
            moveCaret(requireNotNull(comboBox) { "comboBox is null" }.editor.text.length)
            return
        } else if (event.code == KeyCode.DOWN) {
            if (!requireNotNull(comboBox) { "comboBox is null" }.isShowing) {
                comboBox.show()
            }
            caretPos = -1
            moveCaret(comboBox.editor.text.length)
            return
        } else if (event.code == KeyCode.BACK_SPACE) {
            moveCaretToPos = true
            caretPos = requireNotNull(comboBox) { "comboBox is null" }.editor.caretPosition
        } else if (event.code == KeyCode.DELETE) {
            moveCaretToPos = true
            caretPos = requireNotNull(comboBox) { "comboBox is null" }.editor.caretPosition
        }
        if (event.code == KeyCode.RIGHT || event.code == KeyCode.LEFT || event.isControlDown || event.code == KeyCode.HOME || event.code == KeyCode.END || event.code == KeyCode.TAB) {
            return
        }
        val list = FXCollections.observableArrayList<Any>()
        for (i in data.indices) {
            if (comboBox != null) {
                if (data[i].toString().lowercase().startsWith(comboBox.editor.text.lowercase())) {
                    list.add(data[i])
                }
            }
        }
        val t = requireNotNull(comboBox) { "comboBox is null" }.editor.text
        comboBox.items = list
        comboBox.editor.text = t
        if (!moveCaretToPos) {
            caretPos = -1
        }
        moveCaret(t.length)
        if (!list.isEmpty()) {
            comboBox.show()
        }
    }

    private fun moveCaret(textLength: Int) {
        if (caretPos == -1) {
            requireNotNull(comboBox) { "comboBox is null" }.editor.positionCaret(textLength)
        } else {
            requireNotNull(comboBox) { "comboBox is null" }.editor.positionCaret(caretPos)
        }
        moveCaretToPos = false
    }


    init {
        sb = StringBuilder()
        @Suppress("UNCHECKED_CAST")
        data = requireNotNull(comboBox) { "comboBox is null" }.items as ObservableList<T>
        comboBox.isEditable = true
        comboBox.onKeyPressed = EventHandler { comboBox.hide() }
        comboBox.onKeyReleased = this@AutoCompleteComboBoxListener
    }
}