package io.kudos.tools.codegen.service

import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.core.CodeGeneratorContext
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for CodeGenFileService
 *
 * Covers: read (per current context table), save inserting only filenames missing from db
 * (dedup against existing rows), and save with an empty collection. Runs against an in-memory
 * H2 bound via KudosContextHolder.
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeGenFileServiceTest {

    private lateinit var ds: DataSource

    @BeforeTest
    fun setUp() {
        ds = CodegenTestSupport.bindNewH2("tools_svc_file")
        CodegenTestSupport.executeSql(ds, """DELETE FROM "code_gen_file"""")
        CodeGeneratorContext.tableName = "t_user"
    }

    @Test
    fun readReturnsFilenamesOfCurrentTable() {
        CodegenTestSupport.executeSql(
            ds,
            """INSERT INTO "code_gen_file" ("id", "filename", "object_name") VALUES ('f1', 'A.kt', 't_user')""",
            """INSERT INTO "code_gen_file" ("id", "filename", "object_name") VALUES ('f2', 'B.kt', 'other')""",
        )
        assertEquals(listOf("A.kt"), CodeGenFileService.read())
    }

    @Test
    fun saveInsertsOnlyNewFilenames() {
        CodegenTestSupport.executeSql(
            ds,
            """INSERT INTO "code_gen_file" ("id", "filename", "object_name") VALUES ('f1', 'A.kt', 't_user')""",
        )
        assertTrue(CodeGenFileService.save(listOf("A.kt", "C.kt")))

        val rows = CodegenTestSupport.query(
            ds, """SELECT "filename" FROM "code_gen_file" WHERE "object_name" = 't_user'"""
        ) { it.getString(1) }
        assertEquals(setOf("A.kt", "C.kt"), rows.toSet())
        assertEquals(2, rows.size, "already-persisted filename must not be duplicated")
    }

    @Test
    fun saveWithEmptyCollectionSucceedsWithoutInsert() {
        assertTrue(CodeGenFileService.save(emptyList()))
        assertTrue(CodeGenFileService.read().isEmpty())
    }
}
