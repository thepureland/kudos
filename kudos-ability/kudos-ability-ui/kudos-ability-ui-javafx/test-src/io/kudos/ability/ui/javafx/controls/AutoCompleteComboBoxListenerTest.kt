package io.kudos.ability.ui.javafx.controls

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for AutoCompleteComboBoxListener
 *
 * Covers: constructor wiring (editable, onKeyPressed hides, onKeyReleased is the listener),
 * UP/DOWN caret handling and popup opening, BACK_SPACE/DELETE caret preservation, Ctrl and
 * navigation keys skipping the filter, case-insensitive startsWith filtering against the original
 * data (incl. empty prefix, no match, Unicode) and the show/no-show decision.
 *
 * @author K
 * @since 1.0.0
 */
internal class AutoCompleteComboBoxListenerTest {

    private fun newCombo(vararg items: String): ComboBox<Any> =
        ComboBox(FXCollections.observableArrayList<Any>(*items))

    private fun keyReleased(code: KeyCode, ctrl: Boolean = false): KeyEvent =
        KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, ctrl, false, false)

    @Test
    fun constructionMakesComboEditableAndInstallsHandlers() = runFx {
        val combo = newCombo("a")
        val listener = AutoCompleteComboBoxListener<String>(combo)

        assertTrue(combo.isEditable)
        assertSame<Any?>(listener, combo.onKeyReleased)
        assertNotNull(combo.onKeyPressed)
    }

    @Test
    fun keyPressedHandlerHidesPopup() = runFx {
        val combo = newCombo("a")
        AutoCompleteComboBoxListener<String>(combo)
        combo.show()
        assertTrue(combo.isShowing)

        combo.onKeyPressed.handle(KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A, false, false, false, false))

        assertFalse(combo.isShowing)
    }

    @Test
    fun upKeyOnlyMovesCaretToEnd() = runFx {
        val combo = newCombo("Apple", "Banana")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "zzz"

        listener.handle(keyReleased(KeyCode.UP))

        // no filtering happened
        assertEquals(2, combo.items.size)
        assertEquals(3, combo.editor.caretPosition)
    }

    @Test
    fun downKeyOpensPopupAndMovesCaret() = runFx {
        val combo = newCombo("Apple")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "ab"
        assertFalse(combo.isShowing)

        listener.handle(keyReleased(KeyCode.DOWN))

        assertTrue(combo.isShowing)
        assertEquals(2, combo.editor.caretPosition)
        assertEquals(1, combo.items.size)

        // already showing: the show() branch is skipped, behavior stays stable
        listener.handle(keyReleased(KeyCode.DOWN))
        assertTrue(combo.isShowing)
    }

    @Test
    fun typingFiltersItemsCaseInsensitively() = runFx {
        val combo = newCombo("Apple", "Avocado", "Banana")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "A"

        listener.handle(keyReleased(KeyCode.A))

        assertEquals(listOf<Any>("Apple", "Avocado"), combo.items)
        assertTrue(combo.isShowing)
        assertEquals("A", combo.editor.text)
        assertEquals(1, combo.editor.caretPosition)
    }

    @Test
    fun emptyPrefixMatchesAllItems() = runFx {
        val combo = newCombo("Apple", "Banana")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = ""

        listener.handle(keyReleased(KeyCode.BACK_SPACE))

        assertEquals(2, combo.items.size)
        assertTrue(combo.isShowing)
    }

    @Test
    fun noMatchYieldsEmptyItemsAndNoPopup() = runFx {
        val combo = newCombo("Apple", "Banana")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "zzz"

        listener.handle(keyReleased(KeyCode.Z))

        assertTrue(combo.items.isEmpty())
        assertFalse(combo.isShowing)
    }

    @Test
    fun filteringAlwaysUsesOriginalData() = runFx {
        val combo = newCombo("Apple", "Avocado", "Banana")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "a"
        listener.handle(keyReleased(KeyCode.A))
        assertEquals(2, combo.items.size)

        combo.editor.text = "b"
        listener.handle(keyReleased(KeyCode.B))

        // "Banana" is found although the visible items had been narrowed to the "a" matches
        assertEquals(listOf<Any>("Banana"), combo.items)
    }

    @Test
    fun controlShortcutsDoNotFilter() = runFx {
        val combo = newCombo("Apple", "Banana")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "a"

        listener.handle(keyReleased(KeyCode.A, ctrl = true))

        assertEquals(2, combo.items.size)
        assertFalse(combo.isShowing)
    }

    @Test
    fun navigationKeysDoNotFilter() = runFx {
        val combo = newCombo("Apple", "Banana")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "a"

        for (code in listOf(KeyCode.RIGHT, KeyCode.LEFT, KeyCode.HOME, KeyCode.END, KeyCode.TAB)) {
            listener.handle(keyReleased(code))
            assertEquals(2, combo.items.size, "key $code must not trigger filtering")
        }
    }

    @Test
    fun backspacePreservesCaretPosition() = runFx {
        val combo = newCombo("Apple", "Banana")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "ap"
        combo.editor.positionCaret(1)

        listener.handle(keyReleased(KeyCode.BACK_SPACE))

        assertEquals(listOf<Any>("Apple"), combo.items)
        assertEquals(1, combo.editor.caretPosition, "caret must stay where the deletion happened")

        // the next ordinary key moves the caret back to the end again
        combo.editor.text = "ap"
        listener.handle(keyReleased(KeyCode.P))
        assertEquals(2, combo.editor.caretPosition)
    }

    @Test
    fun deletePreservesCaretPosition() = runFx {
        val combo = newCombo("Apple")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "app"
        combo.editor.positionCaret(2)

        listener.handle(keyReleased(KeyCode.DELETE))

        assertEquals(2, combo.editor.caretPosition)
    }

    @Test
    fun unicodePrefixIsFiltered() = runFx {
        val combo = newCombo("蘋果", "香蕉")
        val listener = AutoCompleteComboBoxListener<String>(combo)
        combo.editor.text = "蘋"

        listener.handle(keyReleased(KeyCode.UNDEFINED))

        assertEquals(listOf<Any>("蘋果"), combo.items)
        assertTrue(combo.isShowing)
    }
}
