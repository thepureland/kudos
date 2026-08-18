package io.kudos.tools.codegen.service

import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.core.CodeGeneratorContext
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for CodeGenObjectService
 *
 * Covers:
 * - readTables: tables and views from metadata, exclusion of the code_gen_* bookkeeping tables and
 *   flyway_* tables, and user-supplied comment override from code_gen_object.
 * - saveOrUpdate: insert path on first generation (genCount=1, createUser=author) and update path on
 *   re-generation (genCount incremented, updateUser/updateTime written, comment refreshed).
 *
 * Runs against an in-memory H2 bound via KudosContextHolder.
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeGenObjectServiceTest {

    private lateinit var ds: DataSource

    @BeforeTest
    fun setUp() {
        ds = CodegenTestSupport.bindNewH2(
            "tools_svc_object",
            """CREATE TABLE IF NOT EXISTS "obj_demo" ("id" CHAR(36) NOT NULL PRIMARY KEY, "name" VARCHAR(64))""",
            """COMMENT ON TABLE "obj_demo" IS 'meta comment'""",
            """CREATE TABLE IF NOT EXISTS "flyway_schema_history_test" ("id" INT)""",
            """CREATE VIEW IF NOT EXISTS "obj_demo_view" AS SELECT "id" FROM "obj_demo"""",
        )
        CodegenTestSupport.executeSql(ds, """DELETE FROM "code_gen_object"""")
        CodeGeneratorContext.tableName = "obj_demo"
        CodeGeneratorContext.tableComment = "generated comment"
        CodeGeneratorContext.config = CodegenTestSupport.newConfig(author = "tester")
    }

    @Test
    fun readTablesFiltersBookkeepingAndFlywayTables() {
        val tables = CodeGenObjectService.readTables()
        assertTrue("obj_demo" in tables)
        assertTrue("obj_demo_view" in tables, "views must be included")
        assertFalse("flyway_schema_history_test" in tables)
        assertFalse("code_gen_object" in tables)
        assertFalse("code_gen_file" in tables)
        assertFalse("code_gen_column" in tables)
        assertEquals("meta comment", tables["obj_demo"])
    }

    @Test
    fun readTablesPrefersUserSuppliedComment() {
        CodegenTestSupport.executeSql(
            ds,
            """INSERT INTO "code_gen_object" ("id", "name", "comment", "create_user", "gen_count")
               VALUES ('o1', 'obj_demo', 'user comment', 'K', 1)""",
            """INSERT INTO "code_gen_object" ("id", "name", "comment", "create_user", "gen_count")
               VALUES ('o2', 'gone_table', 'dangling', 'K', 1)""",
        )
        val tables = CodeGenObjectService.readTables()
        assertEquals("user comment", tables["obj_demo"])
        assertFalse("gone_table" in tables, "records of dropped tables must not reappear")
    }

    @Test
    fun saveOrUpdateInsertsThenUpdates() {
        // first generation -> insert path
        assertTrue(CodeGenObjectService.saveOrUpdate())
        val inserted = CodegenTestSupport.query(
            ds,
            """SELECT "comment", "create_user", "gen_count", "update_user" FROM "code_gen_object" WHERE "name" = 'obj_demo'"""
        ) { rs -> listOf(rs.getString(1), rs.getString(2), rs.getInt(3).toString(), rs.getString(4)) }
        assertEquals(1, inserted.size)
        assertEquals(listOf("generated comment", "tester", "1", null), inserted[0])

        // re-generation -> update path
        CodeGeneratorContext.tableComment = "newer comment"
        assertTrue(CodeGenObjectService.saveOrUpdate())
        val updated = CodegenTestSupport.query(
            ds,
            """SELECT "comment", "gen_count", "update_user" FROM "code_gen_object" WHERE "name" = 'obj_demo'"""
        ) { rs -> Triple(rs.getString(1), rs.getInt(2), rs.getString(3)) }
        assertEquals(1, updated.size)
        assertEquals(Triple("newer comment", 2, "tester"), updated[0])
    }

    @Test
    fun bizModuleRoundTripsAndSurvivesARunThatLeavesItBlank() {
        CodeGeneratorContext.bizModule = "dict"
        assertTrue(CodeGenObjectService.saveOrUpdate())
        assertEquals(mapOf("obj_demo" to "dict"), CodeGenObjectService.readBizModules())

        // A later run that leaves the field empty must not wipe the recorded value.
        CodeGeneratorContext.bizModule = ""
        assertTrue(CodeGenObjectService.saveOrUpdate())
        assertEquals(mapOf("obj_demo" to "dict"), CodeGenObjectService.readBizModules())

        CodeGeneratorContext.bizModule = ""
    }

    @Test
    fun readBizModulesOmitsTablesWithoutOne() {
        CodeGeneratorContext.bizModule = ""
        assertTrue(CodeGenObjectService.saveOrUpdate())
        assertTrue(
            CodeGenObjectService.readBizModules().isEmpty(),
            "a blank business module must not be reported, so callers fall back to the default"
        )
    }
}
