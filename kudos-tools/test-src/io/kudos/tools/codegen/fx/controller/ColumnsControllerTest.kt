package io.kudos.tools.codegen.fx.controller

import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.model.vo.ColumnInfo
import io.kudos.tools.codegen.fx.FxTestSupport
import io.kudos.tools.codegen.model.vo.Config
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.CheckBox
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for ColumnsController
 *
 * Loads columns.fxml on the FX application thread to obtain a fully @FXML-injected controller, then drives:
 * - setConfig()/getConfig(): config round-trip and that setConfig triggers table-combo-box init (loads the
 *   table list excluding code_gen_*);
 * - the combo-box editor text listener: selecting a known table fills the comment field, sets the context
 *   table name and asynchronously loads its columns into the table view (waited for via Platform.runLater);
 *   selecting an unknown value only clears the comment and loads nothing;
 * - table / tableComment / columns accessors (incl. trimming and the empty defaults);
 * - selectDetailItems(Event): batch-applies the checkbox state to every column's detailItem;
 * - the "re-init keeps current columns/comment" workaround branch (setConfig called twice).
 *
 * Uses in-memory H2 metadata. All UI interaction runs on the FX thread.
 *
 * @author K
 * @since 1.0.0
 */
internal class ColumnsControllerTest {

    private lateinit var ds: DataSource

    private fun bindContext(): Config {
        ds = CodegenTestSupport.bindNewH2(
            "tools_columns_ctrl",
            """CREATE TABLE IF NOT EXISTS "columns_demo" (
                 "id" CHAR(36) DEFAULT RANDOM_UUID() NOT NULL PRIMARY KEY,
                 "name" VARCHAR(64) NOT NULL,
                 "age" INT
               )""",
        )
        CodegenTestSupport.executeSql(ds, """DELETE FROM "code_gen_column"""")
        return CodegenTestSupport.newConfig(
            templateName = "tpp",
            templateRootDir = CodegenTestSupport.classpathDirPath("/templates/tpp"),
            moduleName = "tpp",
        )
    }

    private fun load(): Pair<Parent, ColumnsController> {
        val loader = FXMLLoader()
        val root = loader.load<Parent>(javaClass.getResourceAsStream("/fxml/columns.fxml"))
        return root to loader.getController()
    }

    /** Runs [block] on the FX thread, then a second runLater to let pending Platform.runLater tasks settle. */
    private fun runFxAndDrain(block: () -> Unit) {
        FxTestSupport.runFx(block)
        // drain: any thread{}->Platform.runLater scheduled inside block will have been posted; flush it.
        val latch = CountDownLatch(1)
        Platform.runLater { latch.countDown() }
        check(latch.await(30, TimeUnit.SECONDS)) { "drain timed out" }
    }

    @Test
    fun setConfigInitsTableComboBoxExcludingCodeGenTables() {
        bindContext()
        runFxAndDrain {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.setConfig(bindContextConfig())
            val items = controller.tableComboBoxItems()
            assertTrue("columns_demo" in items, "business table must be in the combo box")
            assertTrue(items.none { it.startsWith("code_gen_") }, "code_gen_* tables must be excluded")
        }
    }

    @Test
    fun getConfigReturnsInjectedConfig() {
        bindContext()
        val config = bindContextConfig()
        runFxAndDrain {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.setConfig(config)
            assertSame(config, controller.getConfig())
        }
    }

    @Test
    fun emptyAccessorsHaveSafeDefaults() {
        bindContext()
        runFxAndDrain {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.setConfig(bindContextConfig())
            assertNull(controller.table, "no selection -> null table")
            assertEquals("", controller.tableComment, "no comment -> empty string")
            assertTrue(controller.columns.isEmpty(), "no table chosen -> no columns")
        }
    }

    @Test
    fun selectingKnownTableLoadsColumnsAndComment() {
        bindContext()
        val loaded = CountDownLatch(1)
        var controllerRef: ColumnsController? = null
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controllerRef = controller
            controller.setConfig(bindContextConfig())
            // type a known table name into the editable combo box -> triggers the listener
            controller.tableComboBoxEditorText("columns_demo")
        }
        // the listener loads columns on a worker thread then Platform.runLater; poll until items appear
        var attempts = 0
        while (attempts++ < 50) {
            val drained = CountDownLatch(1)
            Platform.runLater { drained.countDown() }
            drained.await(5, TimeUnit.SECONDS)
            val items = onFx { controllerRef!!.columns.size }
            if (items > 0) { loaded.countDown(); break }
            Thread.sleep(50)
        }
        assertTrue(loaded.count == 0L, "columns must be loaded asynchronously for a known table")
        onFx {
            val c = controllerRef!!
            assertEquals("columns_demo", CodeGeneratorContext.tableName)
            assertTrue(c.columns.any { it.getName().equals("name", true) }, "name column must be present")
        }
    }

    @Test
    fun selectingUnknownTableClearsCommentAndLoadsNothing() {
        bindContext()
        runFxAndDrain {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.setConfig(bindContextConfig())
            controller.tableComboBoxEditorText("no_such_table")
            assertEquals("", controller.tableComment, "unknown table -> comment cleared")
            assertTrue(controller.columns.isEmpty(), "unknown table -> no columns loaded")
        }
    }

    @Test
    fun selectDetailItemsBatchAppliesCheckboxState() {
        bindContext()
        runFxAndDrain {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.setConfig(bindContextConfig())
            controller.columnTableItems(
                ColumnInfo().apply { setName("a") },
                ColumnInfo().apply { setName("b") },
            )
            controller.selectDetailItems(actionEventFromCheckBox(true))
            assertTrue(controller.columns.all { it.getDetailItem() })
            controller.selectDetailItems(actionEventFromCheckBox(false))
            assertTrue(controller.columns.none { it.getDetailItem() })
        }
    }

    @Test
    fun reInitKeepsCurrentColumnsAndComment() {
        bindContext()
        runFxAndDrain {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            val config = bindContextConfig()
            controller.setConfig(config)
            // simulate page state: existing columns + selected table + comment
            controller.columnTableItems(ColumnInfo().apply { setName("kept") })
            controller.selectComboItem("columns_demo")
            controller.setCommentText("kept comment")

            // re-init (wizard "back" bug path): must not wipe the existing state
            controller.setConfig(config)

            assertTrue(controller.columns.any { it.getName() == "kept" }, "existing columns must survive re-init")
            assertEquals("kept comment", controller.tableComment, "existing comment must survive re-init")
        }
    }

    // ---- helpers that touch FXML-private state via the controller's public fields ----

    private fun bindContextConfig(): Config = CodegenTestSupport.newConfig(
        templateName = "tpp",
        templateRootDir = CodegenTestSupport.classpathDirPath("/templates/tpp"),
        moduleName = "tpp",
    )

    private fun ColumnsController.tableComboBoxItems(): List<String> =
        tableComboBox.items.map { it.toString() }

    private fun ColumnsController.tableComboBoxEditorText(text: String) {
        tableComboBox.editor.text = text
    }

    private fun ColumnsController.selectComboItem(text: String) {
        tableComboBox.selectionModel.select(text)
    }

    private fun ColumnsController.setCommentText(text: String) {
        tableCommentTextField.text = text
    }

    private fun ColumnsController.columnTableItems(vararg items: ColumnInfo) {
        columnTable.items.setAll(*items)
    }

    private fun <T> onFx(block: () -> T): T {
        var result: T? = null
        val latch = CountDownLatch(1)
        Platform.runLater {
            try { result = block() } finally { latch.countDown() }
        }
        check(latch.await(30, TimeUnit.SECONDS)) { "onFx timed out" }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun actionEventFromCheckBox(selected: Boolean): ActionEvent {
        val cb = CheckBox().apply { isSelected = selected }
        return ActionEvent(cb, cb)
    }
}
