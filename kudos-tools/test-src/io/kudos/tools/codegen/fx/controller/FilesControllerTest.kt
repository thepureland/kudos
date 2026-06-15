package io.kudos.tools.codegen.fx.controller

import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.core.TemplateModelCreator
import io.kudos.tools.codegen.fx.FxTestSupport
import io.kudos.tools.codegen.model.vo.ColumnInfo
import io.kudos.tools.codegen.model.vo.GenFile
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.CheckBox
import javafx.scene.control.TableView
import java.io.File
import java.nio.file.Files
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for FilesController
 *
 * Loads files.fxml on the FX application thread to get a fully @FXML-injected controller, then drives:
 * - initialize(): the "select entity-related files" checkbox is bidirectionally bound to its backing property;
 * - readFiles(): reads the generation file list (entity templates included) and pre-checks files whose name
 *   already exists in code_gen_file;
 * - select(Event): the header select-all / select-none checkbox toggles every file's generate flag;
 * - selectEntityRelativeFiles(Event?): checked -> only entity-related files (by path or body containing
 *   ${entityName}) are selected; unchecked -> all deselected; both the event-driven and programmatic (null
 *   event) entry points;
 * - generate(): no-selection error branch (returns without throwing) and the happy path that renders the
 *   selected templates to disk and shows a success alert;
 * - generateAll(): forces the entity-only toggle off, checks everything and generates every file.
 *
 * Uses the in-memory H2 + the /templates/tpp template set (mix of entity and non-entity templates), the same
 * infrastructure pattern as CodeGeneratorTest. All UI interaction runs on the FX thread.
 *
 * @author K
 * @since 1.0.0
 */
internal class FilesControllerTest {

    private lateinit var ds: DataSource
    private lateinit var outDir: String

    private fun bindContext() {
        ds = CodegenTestSupport.bindNewH2(
            "tools_files_ctrl",
            """CREATE TABLE IF NOT EXISTS "files_demo" (
                 "id" CHAR(36) DEFAULT RANDOM_UUID() NOT NULL PRIMARY KEY,
                 "name" VARCHAR(64) NOT NULL
               )""",
        )
        CodegenTestSupport.executeSql(
            ds,
            """DELETE FROM "code_gen_file"""",
        )
        outDir = Files.createTempDirectory("files-ctrl-out").toFile().absolutePath.replace('\\', '/')
        CodeGeneratorContext.tableName = "files_demo"
        CodeGeneratorContext.tableComment = "files demo"
        CodeGeneratorContext.columns = listOf(ColumnInfo().apply { setName("name") })
        CodeGeneratorContext.config = CodegenTestSupport.newConfig(
            templateName = "tpp",
            templateRootDir = CodegenTestSupport.classpathDirPath("/templates/tpp"),
            moduleName = "tpp",
            codeLocation = outDir,
        )
        CodeGeneratorContext.templateModelCreator = TemplateModelCreator()
    }

    /** Loads files.fxml and returns (root, controller) on the FX thread. */
    private fun load(): Pair<Parent, FilesController> {
        val loader = FXMLLoader()
        val root = loader.load<Parent>(javaClass.getResourceAsStream("/fxml/files.fxml"))
        return root to loader.getController()
    }

    @Test
    fun initializeBindsEntityRelativeCheckBox() {
        FxTestSupport.runFx {
            val (root, controller) = load()
            val checkBox = (root.lookup(".check-box") as? CheckBox)
                ?: root.lookupAll(".check-box").filterIsInstance<CheckBox>().first()
            // toggling the backing property reflects in the checkbox (bidirectional)
            controller.selectEntityRelativeFilesProperty.set(true)
            assertTrue(checkBox.isSelected)
            controller.selectEntityRelativeFilesProperty.set(false)
            assertFalse(checkBox.isSelected)
        }
    }

    @Test
    fun readFilesPreChecksAlreadyGeneratedFilenames() {
        bindContext()
        // pretend "plain.txt" was generated before -> it must be pre-checked
        CodegenTestSupport.executeSql(
            ds,
            """INSERT INTO "code_gen_file" ("id", "filename", "object_name")
               VALUES ('f1', 'plain.txt', 'files_demo')""",
        )
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.readFiles()
            val items = controller.fileTable.items
            assertTrue(items.isNotEmpty(), "tpp templates must produce files")
            val plain = items.first { it.getFilename() == "plain.txt" }
            assertTrue(plain.getGenerate(), "previously generated file must be pre-checked")
            assertTrue(
                items.filter { it.getFilename() != "plain.txt" }.none { it.getGenerate() },
                "files not in code_gen_file must stay unchecked",
            )
        }
    }

    @Test
    fun selectTogglesAllGenerateFlags() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            controller.fileTable.items.setAll(
                GenFile(false, "a.txt", "$tmp/d", "d/a.txt", "d/a.txt"),
                GenFile(false, "b.txt", "$tmp/d", "d/b.txt", "d/b.txt"),
            )
            controller.select(actionEventFrom(selected = true))
            assertTrue(controller.fileTable.items.all { it.getGenerate() })
            controller.select(actionEventFrom(selected = false))
            assertTrue(controller.fileTable.items.none { it.getGenerate() })
        }
    }

    @Test
    fun selectEntityRelativeFilesChecksOnlyEntityRelated() {
        bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.readFiles()
            // start clean
            controller.fileTable.items.forEach { it.setGenerate(false) }

            controller.selectEntityRelativeFilesProperty.set(true)
            controller.selectEntityRelativeFiles(null) // programmatic entry (null event)

            val checked = controller.fileTable.items.filter { it.getGenerate() }.map { it.getFilename() }.toSet()
            // entity templates: FilesDemoDao.kt (by path) and byContent.txt (body contains ${entityName})
            assertTrue("FilesDemoDao.kt" in checked, "entity file by path must be selected")
            assertTrue("byContent.txt" in checked, "entity file by body must be selected")
            assertFalse("plain.txt" in checked, "non-entity file must not be selected")
        }
    }

    @Test
    fun selectEntityRelativeFilesUncheckedDeselectsAll() {
        bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.readFiles()
            controller.fileTable.items.forEach { it.setGenerate(true) }

            controller.selectEntityRelativeFilesProperty.set(false)
            controller.selectEntityRelativeFiles(null)

            assertTrue(controller.fileTable.items.none { it.getGenerate() }, "unchecked must clear all")
        }
    }

    @Test
    fun generateWithNoSelectionReturnsWithoutThrowing() {
        bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.readFiles()
            controller.fileTable.items.forEach { it.setGenerate(false) }
            // must not throw and must not write any file
            controller.generate()
            assertTrue(File(outDir).walkTopDown().none { it.isFile }, "nothing should be generated")
        }
    }

    @Test
    fun generateRendersSelectedFiles() {
        bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.readFiles()
            // select just the plain non-entity file to keep the assertion simple & deterministic
            controller.fileTable.items.forEach { it.setGenerate(it.getFilename() == "plain.txt") }

            controller.generate()

            val generated = File(outDir).walkTopDown().filter { it.isFile }.map { it.name }.toList()
            assertTrue("plain.txt" in generated, "selected file must be generated to disk")
        }
    }

    @Test
    fun generateAllForcesEntityToggleOffAndGeneratesEverything() {
        bindContext()
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.readFiles()
            controller.selectEntityRelativeFilesProperty.set(true)

            controller.generateAll()

            assertFalse(controller.selectEntityRelativeFilesProperty.value, "generateAll must force the toggle off")
            assertTrue(controller.fileTable.items.all { it.getGenerate() }, "generateAll must check every file")
            val generated = File(outDir).walkTopDown().filter { it.isFile }.map { it.name }.toSet()
            assertTrue(generated.containsAll(setOf("plain.txt", "deep.txt", "byContent.txt")), "all files generated")
        }
    }

    @Test
    fun generateSwallowsFailureAndShowsErrorAlert() {
        bindContext()
        // a table with no primary key makes templateModelCreator.create() throw inside generate()
        CodegenTestSupport.executeSql(
            ds,
            """CREATE TABLE IF NOT EXISTS "nopk_files" ("name" VARCHAR(64))""",
        )
        FxTestSupport.runFx {
            CodegenTestSupport.bindDataSourceToCurrentThread(ds)
            val (_, controller) = load()
            controller.readFiles()
            controller.fileTable.items.forEach { it.setGenerate(it.getFilename() == "plain.txt") }
            // switch the context to the PK-less table so model creation throws; generate() must catch
            // (logged + error alert) and not propagate.
            CodeGeneratorContext.tableName = "nopk_files"
            controller.generate() // must not throw
        }
    }

    @Test
    fun fxmlFieldSetterIsCallable() {
        FxTestSupport.runFx {
            val (_, controller) = load()
            val table = TableView<GenFile>()
            controller.fileTable = table
            assertSame(table, controller.fileTable)
        }
    }

    private val tmp: String get() = System.getProperty("java.io.tmpdir").replace('\\', '/').trimEnd('/')

    /** Builds an ActionEvent whose source is a CheckBox with the given selected state. */
    private fun actionEventFrom(selected: Boolean): ActionEvent {
        val cb = CheckBox().apply { isSelected = selected }
        return ActionEvent(cb, cb)
    }
}
