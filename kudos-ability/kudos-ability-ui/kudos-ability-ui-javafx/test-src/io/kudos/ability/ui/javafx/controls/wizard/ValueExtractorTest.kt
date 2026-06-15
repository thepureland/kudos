package io.kudos.ability.ui.javafx.controls.wizard

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.collections.FXCollections
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.PasswordField
import javafx.scene.control.RadioButton
import javafx.scene.control.SelectionMode
import javafx.scene.control.Slider
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.scene.control.TreeView
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for ValueExtractor
 *
 * Covers every registered control type (incl. the single/multiple selection branches for
 * ListView/TreeView/TableView/TreeTableView), the unregistered-type null path and the
 * exact-class (no inheritance) dispatch semantics.
 *
 * @author K
 * @since 1.0.0
 */
internal class ValueExtractorTest {

    @Test
    fun checkBoxYieldsSelectedState() = runFx {
        val cb = CheckBox().apply { isSelected = true }
        assertEquals(true, ValueExtractor.getValue(cb))
        cb.isSelected = false
        assertEquals(false, ValueExtractor.getValue(cb))
    }

    @Test
    fun choiceBoxYieldsValue() = runFx {
        val cb = ChoiceBox(FXCollections.observableArrayList("a", "b"))
        cb.value = "b"
        assertEquals("b", ValueExtractor.getValue(cb))
    }

    @Test
    fun comboBoxYieldsValue() = runFx {
        val cb = ComboBox(FXCollections.observableArrayList("x", "y"))
        cb.value = "x"
        assertEquals("x", ValueExtractor.getValue(cb))
    }

    @Test
    fun datePickerYieldsLocalDate() = runFx {
        val date = LocalDate.of(2026, 6, 11)
        val dp = DatePicker(date)
        assertEquals(date, ValueExtractor.getValue(dp))
    }

    @Test
    fun passwordFieldYieldsText() = runFx {
        val pf = PasswordField().apply { text = "s3cret" }
        assertEquals("s3cret", ValueExtractor.getValue(pf))
    }

    @Test
    fun radioButtonYieldsSelectedState() = runFx {
        val rb = RadioButton().apply { isSelected = true }
        assertEquals(true, ValueExtractor.getValue(rb))
    }

    @Test
    fun sliderYieldsDoubleValue() = runFx {
        val slider = Slider(0.0, 100.0, 25.5)
        assertEquals(25.5, ValueExtractor.getValue(slider))
    }

    @Test
    fun textAreaYieldsText() = runFx {
        val ta = TextArea("multi\nline")
        assertEquals("multi\nline", ValueExtractor.getValue(ta))
    }

    @Test
    fun textFieldYieldsText() = runFx {
        val tf = TextField("")
        assertEquals("", ValueExtractor.getValue(tf))
        tf.text = "值"
        assertEquals("值", ValueExtractor.getValue(tf))
    }

    @Test
    fun listViewSingleSelectionYieldsSelectedItem() = runFx {
        val lv = ListView(FXCollections.observableArrayList("a", "b"))
        lv.selectionModel.select(1)
        assertEquals("b", ValueExtractor.getValue(lv))
    }

    @Test
    fun listViewWithoutSelectionYieldsNull() = runFx {
        val lv = ListView(FXCollections.observableArrayList("a"))
        assertNull(ValueExtractor.getValue(lv))
    }

    @Test
    fun listViewMultipleSelectionYieldsSelectedItems() = runFx {
        val lv = ListView(FXCollections.observableArrayList("a", "b", "c"))
        lv.selectionModel.selectionMode = SelectionMode.MULTIPLE
        lv.selectionModel.selectIndices(0, 2)
        assertEquals(listOf("a", "c"), ValueExtractor.getValue(lv))
    }

    @Test
    fun treeViewSingleSelectionYieldsSelectedItem() = runFx {
        val root = TreeItem("root")
        val tv = TreeView(root)
        tv.selectionModel.select(root)
        assertSame(root, ValueExtractor.getValue(tv))
    }

    @Test
    fun treeViewMultipleSelectionYieldsSelectedItems() = runFx {
        val root = TreeItem("root")
        val tv = TreeView(root)
        tv.selectionModel.selectionMode = SelectionMode.MULTIPLE
        tv.selectionModel.select(root)
        assertEquals(listOf(root), ValueExtractor.getValue(tv))
    }

    @Test
    fun tableViewSingleSelectionYieldsSelectedItem() = runFx {
        val table = TableView(FXCollections.observableArrayList("r1", "r2"))
        table.columns.add(TableColumn<String, String>("c"))
        table.selectionModel.select(0)
        assertEquals("r1", ValueExtractor.getValue(table))
    }

    @Test
    fun tableViewMultipleSelectionYieldsSelectedItems() = runFx {
        val table = TableView(FXCollections.observableArrayList("r1", "r2"))
        table.columns.add(TableColumn<String, String>("c"))
        table.selectionModel.selectionMode = SelectionMode.MULTIPLE
        table.selectionModel.selectIndices(0, 1)
        assertEquals(listOf("r1", "r2"), ValueExtractor.getValue(table))
    }

    @Test
    fun treeTableViewSingleSelectionYieldsSelectedItem() = runFx {
        val root = TreeItem("root")
        val ttv = TreeTableView(root)
        ttv.columns.add(TreeTableColumn<String, String>("c"))
        ttv.selectionModel.select(0)
        assertSame(root, ValueExtractor.getValue(ttv))
    }

    @Test
    fun treeTableViewMultipleSelectionYieldsSelectedItems() = runFx {
        val root = TreeItem("root")
        val ttv = TreeTableView(root)
        ttv.columns.add(TreeTableColumn<String, String>("c"))
        ttv.selectionModel.selectionMode = SelectionMode.MULTIPLE
        ttv.selectionModel.select(0)
        assertEquals(listOf(root), ValueExtractor.getValue(ttv))
    }

    @Test
    fun unregisteredControlTypeYieldsNull() = runFx {
        assertNull(ValueExtractor.getValue(Label("no extractor")))
    }

    @Test
    fun dispatchIsByExactClassWithoutInheritanceExpansion() = runFx {
        // anonymous TextField subclass is not registered, so no value is extracted
        val subclass = object : TextField("text") {}
        assertNull(ValueExtractor.getValue(subclass))
    }
}
