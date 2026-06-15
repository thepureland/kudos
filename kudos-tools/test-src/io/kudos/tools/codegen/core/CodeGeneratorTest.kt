package io.kudos.tools.codegen.core

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.model.vo.ColumnInfo
import io.kudos.tools.codegen.model.vo.GenFile
import org.ktorm.database.Database
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.sql.Connection
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for CodeGenerator
 *
 * End-to-end coverage of the generation flow against an in-memory H2 and real template files:
 * - first generation (target file absent): touch -> Freemarker render -> PrivateContentEraser strips
 *   the append-region markers; generation record persisted (insert path, only generate=true files).
 * - re-generation (target file present): CodeMerger restores user code in regions, appends the
 *   template's append-codes, and merges user-added imports; persistence update path (genCount++).
 * - generate(needPersist=false): no generation record written.
 * - persistence failure paths (via a JDBC proxy that reports zero affected rows): failure at the
 *   object step skips the column/file steps, failure at the column step skips the file step; in
 *   both cases generate() only logs a warning and the generated files remain on disk.
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeGeneratorTest {

    private lateinit var ds: DataSource
    private lateinit var outDir: String

    private val templateModel = mapOf<String, Any?>("greeting" to "hello")

    @BeforeTest
    fun setUp() {
        ds = CodegenTestSupport.bindNewH2("tools_cgen")
        CodegenTestSupport.executeSql(
            ds,
            """DELETE FROM "code_gen_object"""",
            """DELETE FROM "code_gen_column"""",
            """DELETE FROM "code_gen_file"""",
        )
        outDir = Files.createTempDirectory("cgen-out").toFile().absolutePath.replace('\\', '/')
        CodeGeneratorContext.tableName = "cgen_demo"
        CodeGeneratorContext.tableComment = "demo table"
        CodeGeneratorContext.columns = listOf(
            ColumnInfo().apply {
                setName("name")
                setSearchItem(true)
            }
        )
        CodeGeneratorContext.config = CodegenTestSupport.newConfig(
            templateName = "cgen",
            templateRootDir = CodegenTestSupport.classpathDirPath("/templates/cgen"),
            codeLocation = outDir,
        )
    }

    private fun newGenFiles() = listOf(
        GenFile(true, "demo.kt", "$outDir/out", "out/demo.kt", "out/demo.kt"),
        GenFile(false, "skip.txt", "$outDir/out", "out/skip.txt", "out/skip.txt"),
    )

    @Test
    fun generateCreatesMergesAndPersists() {
        val demoFile = java.io.File("$outDir/out/demo.kt")
        val skipFile = java.io.File("$outDir/out/skip.txt")

        // ---- first generation: files are created and append markers erased ----
        CodeGenerator(templateModel, newGenFiles()).generate()

        val firstContent = demoFile.readText()
        assertTrue(firstContent.contains("val appended = \"hello\""), "placeholder must be rendered")
        assertFalse(firstContent.contains("region append"), "append markers must be erased on first generation")
        assertTrue(firstContent.contains("//region your codes 1"), "user regions must be kept")
        assertEquals("skip hello\n", skipFile.readText().replace("\r\n", "\n"), "generate flag does not skip rendering")

        // persistence insert path: only generate=true filenames recorded
        assertEquals(
            listOf(Pair("cgen_demo", 1)),
            CodegenTestSupport.query(ds, """SELECT "name", "gen_count" FROM "code_gen_object"""") {
                Pair(it.getString(1), it.getInt(2))
            }
        )
        assertEquals(
            listOf("demo.kt"),
            CodegenTestSupport.query(ds, """SELECT "filename" FROM "code_gen_file"""") { it.getString(1) }
        )
        assertEquals(
            listOf("name"),
            CodegenTestSupport.query(ds, """SELECT "name" FROM "code_gen_column"""") { it.getString(1) }
        )

        // ---- user edits the generated file ----
        val edited = firstContent
            .replace("import t.T", "import t.T\nimport extra.X")
            .replace(
                "//region your codes 1",
                "//region your codes 1\n    val custom = 9"
            )
        demoFile.writeText(edited)

        // ---- re-generation: user code and imports merged back, record updated ----
        CodeGenerator(templateModel, newGenFiles()).generate()

        val merged = demoFile.readText()
        assertTrue(merged.contains("val custom = 9"), "user code inside the region must survive re-generation")
        assertTrue(merged.contains("import extra.X"), "user-added import must survive re-generation")
        assertTrue(merged.contains("val appended = \"hello\""), "append codes must be appended into the region")
        assertEquals(1, Regex("import t\\.T").findAll(merged).count(), "template import must not be duplicated")

        assertEquals(
            listOf(2),
            CodegenTestSupport.query(ds, """SELECT "gen_count" FROM "code_gen_object"""") { it.getInt(1) },
            "re-generation must take the update path"
        )
        assertEquals(
            listOf("demo.kt"),
            CodegenTestSupport.query(ds, """SELECT "filename" FROM "code_gen_file"""") { it.getString(1) },
            "already recorded filename must not be duplicated"
        )

        // ---- needPersist=false leaves the record untouched ----
        CodeGenerator(templateModel, newGenFiles()).generate(needPersist = false)
        assertEquals(
            listOf(2),
            CodegenTestSupport.query(ds, """SELECT "gen_count" FROM "code_gen_object"""") { it.getInt(1) },
            "needPersist=false must not write the generation record"
        )
    }

    @Test
    fun persistenceFailureAtObjectStepOnlyWarnsAndSkipsLaterSteps() {
        // an existing object row forces the update path; the sabotaged UPDATE reports 0 affected rows
        CodegenTestSupport.executeSql(
            ds,
            """INSERT INTO "code_gen_object" ("id", "name", "comment", "create_user", "gen_count")
               VALUES ('o1', 'cgen_demo', 'old', 'K', 1)""",
            """INSERT INTO "code_gen_column" ("id", "name", "object_name") VALUES ('keep', 'legacy', 'cgen_demo')""",
        )
        bindSabotagedDatabase { sql -> sql.trimStart().startsWith("UPDATE", true) && sql.contains("code_gen_object") }

        // must not throw: the failed persistence is only logged as a warning
        CodeGenerator(templateModel, newGenFiles()).generate()

        assertTrue(java.io.File("$outDir/out/demo.kt").exists(), "files must still be generated")
        assertEquals(
            listOf(1),
            CodegenTestSupport.query(ds, """SELECT "gen_count" FROM "code_gen_object"""") { it.getInt(1) },
            "the sabotaged update must not change the row"
        )
        assertEquals(
            listOf("legacy"),
            CodegenTestSupport.query(ds, """SELECT "name" FROM "code_gen_column"""") { it.getString(1) },
            "the column step must be skipped after the object step fails"
        )
        assertTrue(
            CodegenTestSupport.query(ds, """SELECT "filename" FROM "code_gen_file"""") { it.getString(1) }.isEmpty(),
            "the file step must be skipped after the object step fails"
        )
    }

    @Test
    fun persistenceFailureAtColumnStepOnlyWarnsAndSkipsFileStep() {
        // no existing object row: the object step succeeds via the insert path,
        // then the sabotaged batch INSERT into code_gen_column reports 0 inserted rows
        bindSabotagedDatabase { sql -> sql.trimStart().startsWith("INSERT", true) && sql.contains("code_gen_column") }

        // must not throw: the failed persistence is only logged as a warning
        CodeGenerator(templateModel, newGenFiles()).generate()

        assertEquals(
            listOf(Pair("cgen_demo", 1)),
            CodegenTestSupport.query(ds, """SELECT "name", "gen_count" FROM "code_gen_object"""") {
                Pair(it.getString(1), it.getInt(2))
            },
            "the object step must succeed before the column step fails"
        )
        assertTrue(
            CodegenTestSupport.query(ds, """SELECT "name" FROM "code_gen_column"""") { it.getString(1) }.isEmpty(),
            "the sabotaged batch insert must not write any column row"
        )
        assertTrue(
            CodegenTestSupport.query(ds, """SELECT "filename" FROM "code_gen_file"""") { it.getString(1) }.isEmpty(),
            "the file step must be skipped after the column step fails"
        )
    }

    //region JDBC sabotage proxy

    /**
     * Rebinds the context Database to a proxied DataSource whose statements report "0 affected rows"
     * (without executing anything) for every SQL matched by [shouldSabotage]. This is the only way to
     * drive BaseCrudDao.update/batchInsert into returning false deterministically.
     */
    private fun bindSabotagedDatabase(shouldSabotage: (String) -> Boolean) {
        val realDs = ds
        val dsProxy = newProxy(DataSource::class.java, realDs) { method, _, invokeReal ->
            if (method.name == "getConnection") {
                val conn = invokeReal() as Connection
                newProxy(Connection::class.java, conn) { m2, a2, invokeReal2 ->
                    val sql = a2?.firstOrNull() as? String
                    if (m2.name == "prepareStatement" && sql != null && shouldSabotage(sql)) {
                        val ps = invokeReal2() as java.sql.PreparedStatement
                        newProxy(java.sql.PreparedStatement::class.java, ps) { m3, _, invokeReal3 ->
                            when (m3.name) {
                                "executeUpdate" -> 0
                                "executeLargeUpdate" -> 0L
                                "executeBatch" -> IntArray(0)
                                "executeLargeBatch" -> LongArray(0)
                                else -> invokeReal3()
                            }
                        }
                    } else invokeReal2()
                }
            } else invokeReal()
        }
        KudosContextHolder.get().addOtherInfos(
            KudosContext.OTHER_INFO_KEY_DATA_SOURCE to dsProxy,
            KudosContext.OTHER_INFO_KEY_DATABASE to Database.connect(dsProxy, alwaysQuoteIdentifiers = true)
        )
    }

    private fun <T> newProxy(
        iface: Class<T>,
        target: Any,
        handler: (Method, Array<Any?>?, () -> Any?) -> Any?
    ): T = iface.cast(
        Proxy.newProxyInstance(iface.classLoader, arrayOf(iface)) { _, method, args ->
            handler(method, args) {
                try {
                    if (args == null) method.invoke(target) else method.invoke(target, *args)
                } catch (e: InvocationTargetException) {
                    throw e.targetException
                }
            }
        }
    )

    //endregion
}
