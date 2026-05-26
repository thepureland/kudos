package io.kudos.ability.ui.javafx.controls

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.ComboBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

/**
 * Adds "filter as you type" behavior to an editable [ComboBox]:
 *
 * - Listens to `onKeyReleased` and performs a case-insensitive startsWith filter using the
 *   current editor text.
 * - Arrow keys / Ctrl / HOME / END / TAB skip filtering and only move the caret.
 * - Preserves the caret position after BACK_SPACE / DELETE so it does not jump to the end on
 *   every keystroke.
 *
 * **Construction registers it** — the `init` block sets the ComboBox to editable and attaches
 * the onKeyPressed (hide) and onKeyReleased (this instance) listeners. So the business side
 * only needs:
 *
 * ```kotlin
 * val combo = ComboBox<Any>().apply { items = ... }
 * AutoCompleteComboBoxListener<String>(combo)  // takes effect on construction
 * ```
 *
 * The type parameter `T` is the actual element type of the items; the ComboBox itself is
 * declared with `<Any>` for historical reasons — `data[i].toString()` provides the display
 * string and does not depend on `T`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AutoCompleteComboBoxListener<T>(private val comboBox: ComboBox<Any>) : EventHandler<KeyEvent> {

    /** Reference to the ComboBox's original items; filtering shows a copy to avoid mutating the source. */
    private val data: ObservableList<T>
    /** Whether the caret needs to be moved to [caretPos] after BACK_SPACE/DELETE so it does not jump to the end. */
    private var moveCaretToPos = false
    /** Temporary caret position; -1 means "no preservation needed; let the caret follow the text length". */
    private var caretPos = 0

    init {
        @Suppress("UNCHECKED_CAST")
        data = comboBox.items as ObservableList<T>
        comboBox.isEditable = true
        comboBox.onKeyPressed = EventHandler { comboBox.hide() }
        comboBox.onKeyReleased = this@AutoCompleteComboBoxListener
    }

    /**
     * Handles the `onKeyReleased` event: implements the "filter as you type" logic.
     *
     * Arrow keys / Ctrl / HOME / END / TAB return immediately (caret navigation only, no
     * filtering). BACK_SPACE / DELETE first record the current caretPos so that [moveCaret]
     * can preserve the position. Other keys filter items by startsWith on the editor text and
     * show the dropdown when the result is non-empty.
     *
     * @param event keyboard event
     * @author K
     * @since 1.0.0
     */
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
        if (event.isControlDown || event.code in SKIP_CODES) return
        val prefix = comboBox.editor.text.lowercase()
        // .lowercase() without a Locale uses Locale.ROOT (Kotlin 1.5+) — avoids the Turkish
        // locale i→İ false match, which desktop UI input comparison relies on.
        val matched = data.filter { it.toString().lowercase().startsWith(prefix) }
        val list: ObservableList<Any> = FXCollections.observableArrayList<Any>().apply { addAll(matched) }
        val t = comboBox.editor.text
        comboBox.items = list
        comboBox.editor.text = t
        if (!moveCaretToPos) caretPos = -1
        moveCaret(t.length)
        if (list.isNotEmpty()) comboBox.show()
    }

    /**
     * Positions the caret appropriately: when [caretPos] is -1, move to the end; otherwise keep
     * the position from before the deletion.
     * Finally resets [moveCaretToPos] to false so the next BACK_SPACE/DELETE re-enters
     * "preserve position" mode.
     *
     * @param textLength current text length; used by the "move to end" mode
     * @author K
     * @since 1.0.0
     */
    private fun moveCaret(textLength: Int) {
        comboBox.editor.positionCaret(if (caretPos == -1) textLength else caretPos)
        moveCaretToPos = false
    }

    private companion object {
        private val SKIP_CODES = setOf(KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB)
    }

}
