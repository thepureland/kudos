package io.kudos.tools.codegen.fx.controller

import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.core.TemplateModelCreator
import io.kudos.tools.codegen.fx.FxTestSupport
import io.kudos.tools.codegen.model.vo.Config
import io.kudos.tools.codegen.model.vo.DbTable
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.CheckBox
import java.io.File
import java.nio.file.Files
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for BatchGenerationController
 *
 * Loads batch.fxml on the FX application thread to obtain a fully @FXML-injected controller, then drives:
 * - initialize(): the "only entity-related files" checkbox is bound to its backing property and defaults to
 *   checked;
 * - setConfig()/getConfig(): config round-trip;
 * - initTable(): reads table names/comments from metadata into the table view (excluding code_gen_* and
 *   flyway_*), nothing pre-checked;
 * - select(Event): the header checkbox toggles every DbTable.generate flag;
 * - generate(): no-selection error branch (returns without throwing); the happy path with the entity-only
 *   toggle ON (only entity-related templates rendered) and OFF (table-independent files also rendered).
 *
 * Uses in-memory H2 + the /templates/tpp template set. All UI interaction runs on the FX thread.
 *
 * @author K
 * @since 1.0.0
 */
internal class BatchGenerationControllerTest {

    private lateinit var ds: DataSource
    private lateinit var outDir: String

    private fun bindContext(): Config {
        ds = CodegenTestSupport.bindNewH2(
            "tools_batch_ctrl",
            """CREATE TABLE IF NOT EXISTS "batch_demo" (
                 "id" CHAR(36) DEFAULT RANDOM_UUID() NOT NULL PRIMARY KEY,
                 "name" VARCHAR(64) NOT NULL
               )""",
        )
        CodegenTestSupport.executeSql(
            ds,
            """DELETE FROM "code_gen_object"""",
            """DELETE FROM "code_gen_column"""",
            """DELETE FROM "code_gen_file"""",
        )
        outDir = Files.createTempDirectory("batch-ctrl-out").toFile().absolutePath.replace('\\', '/')
        CodeGeneratorContext.templateModelCreator = TemplateModelCreator()
        return CodegenTestSupport.newConfig(
            templateName = "tpp",
            templateRootDir = CodegenTestSupport.classpathDirPath("/templates/tpp"),
            moduleName = "tpp",
            codeLocation = outDir,
        )
    }

    private fun load(): Pair<Parent, BatchGenerationController> {
        val loader = FXMLLoader()
        val root = loader.load<Parent>(javaClass.getResourceAsStream("/fxml/batch.fxml"))
        return root to loader.getController()
    }

    private fun checkBoxOf(root: Parent): CheckBox =
        root.lookupAll(".check-box").filterIsInstance<CheckBox>().first()

    @Test
    fun initializeDefaultsOnlyEntityCheckBoxToChecked() {
        FxTestSupport.runFx {
            val (root, _) = load()
            assertTrue(checkBoxOf(root).isSelected, "only-entity-related checkbox must default to checked")
        }
    }

    @Test
    fun setAndGetConfigRoundTrip() {
        val config = bindContext()
        FxTestSupport.runFx {
            val (_, controller) = load()
            controller.setConfig(config)
            assertSame(config, controller.getConfig())
        }
    }

    @Test
    fun initTableLoadsBusinessTablesUnchecked() {
        bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.initTable()
            val names = controller.entityTable.items.map { it.getTableName() }
            assertTrue("batch_demo" in names, "business table must be listed")
            assertTrue(names.none { it.startsWith("code_gen_") }, "code_gen_* tables must be excluded")
            assertTrue(names.none { it.startsWith("flyway_") }, "flyway_* tables must be excluded")
            assertTrue(controller.entityTable.items.none { it.getGenerate() }, "nothing pre-checked")
            assertTrue(
                controller.entityTable.items.all { it.getBizModule().isEmpty() },
                "without a config there is no module name to strip, so the business module is left blank " +
                    "and TemplateModelCreator resolves the default at render time"
            )
        }
    }

    @Test
    fun initTablePrefillsBusinessModuleFromTheModuleName() {
        val config = bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.setConfig(config)
            controller.initTable()
            val demo = controller.entityTable.items.first { it.getTableName() == "batch_demo" }
            assertEquals(
                TemplateModelCreator.defaultBizModule("batch_demo", config.getModuleName()),
                demo.getBizModule()
            )
        }
    }

    @Test
    fun selectTogglesAllTables() {
        bindContext()
        FxTestSupport.runFx {
            val (_, controller) = load()
            controller.entityTable.items.setAll(
                DbTable(false, "t1", "c1"),
                DbTable(false, "t2", null),
            )
            controller.select(actionEventFrom(true))
            assertTrue(controller.entityTable.items.all { it.getGenerate() })
            controller.select(actionEventFrom(false))
            assertTrue(controller.entityTable.items.none { it.getGenerate() })
        }
    }

    @Test
    fun generateWithNoSelectionReturnsWithoutThrowing() {
        val config = bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.setConfig(config)
            controller.initTable()
            controller.entityTable.items.forEach { it.setGenerate(false) }
            controller.generate()
            assertTrue(File(outDir).walkTopDown().none { it.isFile }, "nothing should be generated")
        }
    }

    @Test
    fun generateEntityOnlyRendersOnlyEntityRelatedFiles() {
        val config = bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (root, controller) = load()
            controller.setConfig(config)
            controller.initTable()
            controller.entityTable.items.first { it.getTableName() == "batch_demo" }.setGenerate(true)
            // checkbox defaults to checked => entity-only
            assertTrue(checkBoxOf(root).isSelected)

            controller.generate()

            val generated = File(outDir).walkTopDown().filter { it.isFile }.map { it.name }.toSet()
            assertTrue("BatchDemoDao.kt" in generated, "entity file must be generated")
            assertTrue("byContent.txt" in generated, "entity-by-body file must be generated")
            assertFalse("plain.txt" in generated, "table-independent file must NOT be generated in entity-only mode")
        }
    }

    @Test
    fun generateWithEntityToggleOffAlsoRendersTableIndependentFiles() {
        val config = bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (root, controller) = load()
            controller.setConfig(config)
            controller.initTable()
            controller.entityTable.items.first { it.getTableName() == "batch_demo" }.setGenerate(true)
            checkBoxOf(root).isSelected = false // turn off entity-only

            controller.generate()

            val generated = File(outDir).walkTopDown().filter { it.isFile }.map { it.name }.toSet()
            assertTrue("plain.txt" in generated, "table-independent file must be generated when toggle is off")
            assertTrue("BatchDemoDao.kt" in generated, "entity file must still be generated")
        }
    }

    @Test
    fun generateSwallowsFailureAndShowsErrorAlert() {
        val config = bindContext()
        // a table with no primary key makes createEntityRelativeModel().first{ primaryKey } throw
        CodegenTestSupport.executeSql(
            ds,
            """CREATE TABLE IF NOT EXISTS "nopk_demo" ("name" VARCHAR(64))""",
        )
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.setConfig(config)
            controller.initTable()
            // entity-only mode (default) + a PK-less table -> model creation throws inside generate(),
            // which must catch (logged + error alert) and not propagate.
            controller.entityTable.items.first { it.getTableName() == "nopk_demo" }.setGenerate(true)
            controller.generate() // must not throw
        }
    }

    @Test
    fun fxmlFieldSetterIsCallable() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            val table = javafx.scene.control.TableView<DbTable>()
            controller.entityTable = table
            assertSame(table, controller.entityTable)
        }
    }

    private fun actionEventFrom(selected: Boolean): ActionEvent {
        val cb = CheckBox().apply { isSelected = selected }
        return ActionEvent(cb, cb)
    }
}
