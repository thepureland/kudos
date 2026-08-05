package io.kudos.ability.ui.javafx.controls

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.scene.Scene
import javafx.scene.control.Cell
import javafx.scene.control.ContentDisplay
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import javafx.util.StringConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for TextFieldCell
 *
 * Covers: init block (zero padding style, always-present TextField graphic with default style),
 * the instance updateItem for non-empty cells with/without converter and for empty cells, all
 * branches of the companion updateItem helper (empty cell, view mode with/without converter and
 * with null item, edit mode with graphic+textField, textField only and null textField) and the
 * focus/hover style listeners (toggled via the protected Node setters).
 *
 * @author K
 * @since 1.0.0
 */
internal class TextFieldCellTest {

    /** Exposes the protected Cell.updateItem so cell state can be staged for the companion helper. */
    private class StatefulCell : Cell<String?>() {
        fun setState(item: String?, empty: Boolean) = updateItem(item, empty)
    }

    private val prefixConverter = object : StringConverter<String?>() {
        override fun toString(obj: String?): String = "C:$obj"
        override fun fromString(string: String?): String? = string
    }

    private fun invokeUpdateItem(cell: TextFieldCell<*, *>, item: Any?, empty: Boolean) {
        val m = TextFieldCell::class.java.getDeclaredMethod(
            "updateItem", Any::class.java, Boolean::class.javaPrimitiveType
        )
        m.isAccessible = true
        m.invoke(cell, item, empty)
    }

    @Test
    fun initInstallsTextFieldGraphicWithDefaultStyle() = runFx {
        val cell = TextFieldCell<Any, String>()

        assertEquals("-fx-padding: 0;", cell.style)
        val tf = cell.graphic
        assertTrue(tf is TextField)
        assertTrue(tf.style.contains("-fx-control-inner-background"))
        assertTrue(tf.style.contains("-fx-cursor: text;"))
    }

    @Test
    fun updateItemNonEmptyWithoutConverterCastsItemToString() = runFx {
        val cell = TextFieldCell<Any, String>()

        invokeUpdateItem(cell, "hello", false)

        assertEquals(ContentDisplay.GRAPHIC_ONLY, cell.contentDisplay)
        assertEquals("hello", (cell.graphic as TextField).text)
    }

    @Test
    fun updateItemNonEmptyWithConverterUsesConverter() = runFx {
        val sc = object : StringConverter<Any>() {
            override fun toString(obj: Any?): String = "n=$obj"
            override fun fromString(string: String?): Any = string ?: ""
        }
        val cell = TextFieldCell<Any, Int>(sc)

        invokeUpdateItem(cell, 42, false)

        assertEquals("n=42", (cell.graphic as TextField).text)
    }

    @Test
    fun updateItemEmptyFallsBackToTextOnly() = runFx {
        val cell = TextFieldCell<Any, String>()

        invokeUpdateItem(cell, null, true)

        assertEquals(ContentDisplay.TEXT_ONLY, cell.contentDisplay)
    }

    @Test
    fun companionUpdateItemClearsEmptyCell() = runFx {
        val cell = StatefulCell()
        cell.setState(null, true)

        TextFieldCell.updateItem(cell, null, HBox(), Rectangle(), TextField())

        assertNull(cell.text)
        assertNull(cell.graphic)
    }

    @Test
    fun companionUpdateItemViewModeWithoutConverterUsesToString() = runFx {
        val cell = StatefulCell()
        cell.setState("v", false)
        val icon = Rectangle()

        TextFieldCell.updateItem(cell, null, HBox(), icon, null)

        assertEquals("v", cell.text)
        assertSame(icon, cell.graphic)
    }

    @Test
    fun companionUpdateItemViewModeNullItemYieldsEmptyText() = runFx {
        val cell = StatefulCell()
        cell.setState(null, false)

        TextFieldCell.updateItem(cell, null, HBox(), null, null)

        assertEquals("", cell.text)
        assertNull(cell.graphic)
    }

    @Test
    fun companionUpdateItemViewModeWithConverter() = runFx {
        val cell = StatefulCell()
        cell.setState("v", false)

        TextFieldCell.updateItem(cell, prefixConverter, HBox(), null, null)

        assertEquals("C:v", cell.text)
    }

    @Test
    fun companionUpdateItemEditModeWithGraphicWrapsInHBox() = runFx {
        val cell = StatefulCell()
        cell.setState("v", false)
        cell.startEdit()
        assertTrue(cell.isEditing)
        val hbox = HBox()
        val icon = Rectangle()
        val tf = TextField()

        TextFieldCell.updateItem(cell, prefixConverter, hbox, icon, tf)

        assertEquals("C:v", tf.text)
        assertNull(cell.text)
        assertEquals(listOf(icon, tf), hbox.children)
        assertSame(hbox, cell.graphic)
    }

    @Test
    fun companionUpdateItemEditModeWithoutGraphicShowsTextFieldOnly() = runFx {
        val cell = StatefulCell()
        cell.setState("v", false)
        cell.startEdit()
        val tf = TextField()

        TextFieldCell.updateItem(cell, prefixConverter, HBox(), null, tf)

        assertEquals("C:v", tf.text)
        assertSame(tf, cell.graphic)
    }

    @Test
    fun companionUpdateItemEditModeWithNullTextField() = runFx {
        val cell = StatefulCell()
        cell.setState("v", false)
        cell.startEdit()

        TextFieldCell.updateItem(cell, prefixConverter, HBox(), null, null)

        assertNull(cell.text)
        assertNull(cell.graphic)
    }

    /** Invokes the internal Node.setHover(boolean) so the hover listener fires deterministically. */
    private fun setHover(node: javafx.scene.Node, hover: Boolean) {
        val m = javafx.scene.Node::class.java.getDeclaredMethod("setHover", Boolean::class.javaPrimitiveType)
        m.isAccessible = true
        m.invoke(node, hover)
    }

    @Test
    fun focusListenerSwitchesBetweenFocusAndDefaultStyle() = runFx {
        val cell = TextFieldCell<Any, String>()
        val tf = cell.graphic as TextField
        // place the field on a shown stage so requesting focus actually flips focusedProperty
        val stage = Stage()
        stage.scene = Scene(VBox(tf))
        stage.show()
        try {
            tf.requestFocus()
            assertTrue(tf.isFocused, "field must own focus after requestFocus on a shown stage")
            assertTrue(tf.style.contains("purple"), "focused style applied")

            // move focus away -> falls back to the default style
            val other = TextField()
            (tf.scene.root as VBox).children.add(other)
            other.requestFocus()
            assertEquals(false, tf.isFocused)
            assertTrue(tf.style.contains("-fx-control-inner-background"))
            assertTrue(!tf.style.contains("purple, "), "default style restored after focus lost")
        } finally {
            stage.close()
        }
    }

    @Test
    fun hoverListenerAppliesHoverThenFallsBackToFocusOrDefault() = runFx {
        val cell = TextFieldCell<Any, String>()
        val tf = cell.graphic as TextField

        // hover on -> hover style (light purple)
        setHover(tf, true)
        assertTrue(tf.style.contains("derive(purple,90%)"), "hover style applied")

        // hover off while not focused -> default style branch
        setHover(tf, false)
        assertEquals(false, tf.isFocused)
        assertTrue(tf.style.contains("-fx-control-inner-background"))
        assertTrue(!tf.style.contains("derive(purple,90%)"))

        // hover off while focused -> focus style branch
        val stage = Stage()
        stage.scene = Scene(VBox(tf))
        stage.show()
        try {
            tf.requestFocus()
            assertTrue(tf.isFocused)
            setHover(tf, true)
            assertTrue(tf.style.contains("derive(purple,90%)"))
            setHover(tf, false)
            // focused-and-not-hovered -> the STYLE_HAS_FOCUS branch
            assertTrue(tf.style.contains("purple, -fx-text-box-border"), "focus style on hover-out while focused")
        } finally {
            stage.close()
        }
    }
}
