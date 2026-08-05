package io.kudos.tools.codegen.service

import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.model.vo.ColumnInfo
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for CodeGenColumnService
 *
 * Covers:
 * - readColumns first-time path (no code_gen_column rows): defaults applied — detail/cache on for all
 *   columns, search/list/edit toggled via the SKIP_* lists (id, built_in, create_user...).
 * - readColumns merge path (existing code_gen_column rows): flags/custom comment restored from db,
 *   orig comment from metadata, defaults NOT applied; columns without saved config get name+origComment only.
 * - saveColumns: replaces old rows, persists all flags, returns true; null column name rejected.
 *
 * Runs against an in-memory H2 bound via KudosContextHolder.
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeGenColumnServiceTest {

    private lateinit var ds: DataSource

    @BeforeTest
    fun setUp() {
        ds = CodegenTestSupport.bindNewH2(
            "tools_svc_column",
            """CREATE TABLE IF NOT EXISTS "svc_demo" (
                 "id" CHAR(36) DEFAULT RANDOM_UUID() NOT NULL PRIMARY KEY,
                 "name" VARCHAR(64) NOT NULL,
                 "built_in" BOOLEAN DEFAULT FALSE NOT NULL,
                 "create_user" VARCHAR(36)
               )""",
            """COMMENT ON COLUMN "svc_demo"."name" IS 'orig name comment'""",
        )
        CodegenTestSupport.executeSql(ds, """DELETE FROM "code_gen_column"""")
    }

    @Test
    fun readColumnsAppliesDefaultsWhenNoSavedConfig() {
        val columns = CodeGenColumnService.readColumns("svc_demo").associateBy { it.getName() }
        assertEquals(setOf("id", "name", "built_in", "create_user"), columns.keys)

        val id = columns.getValue("id")
        assertTrue(id.getDetailItem())
        assertTrue(id.getCacheItem())
        assertFalse(id.getSearchItem())
        assertFalse(id.getListItem())
        assertFalse(id.getEditItem())

        val name = columns.getValue("name")
        assertTrue(name.getSearchItem())
        assertTrue(name.getListItem())
        assertTrue(name.getEditItem())
        assertEquals("orig name comment", name.getOrigComment())

        // built_in is only skipped for search/edit, not for list
        val builtIn = columns.getValue("built_in")
        assertFalse(builtIn.getSearchItem())
        assertTrue(builtIn.getListItem())
        assertFalse(builtIn.getEditItem())

        val createUser = columns.getValue("create_user")
        assertFalse(createUser.getSearchItem())
        assertFalse(createUser.getListItem())
        assertFalse(createUser.getEditItem())
    }

    @Test
    fun readColumnsMergesSavedConfigAndSkipsDefaults() {
        CodegenTestSupport.executeSql(
            ds,
            """INSERT INTO "code_gen_column" ("id", "name", "object_name", "comment", "search_item", "list_item")
               VALUES ('c1', 'name', 'svc_demo', 'custom comment', true, true)""",
        )
        val columns = CodeGenColumnService.readColumns("svc_demo").associateBy { it.getName() }

        val name = columns.getValue("name")
        assertEquals("custom comment", name.getCustomComment())
        assertEquals("orig name comment", name.getOrigComment())
        assertTrue(name.getSearchItem())
        assertTrue(name.getListItem())
        assertFalse(name.getEditItem())

        // columns without saved config get only name + origComment, and no defaults since the map is non-empty
        val id = columns.getValue("id")
        assertFalse(id.getDetailItem())
        assertFalse(id.getCacheItem())
        assertFalse(id.getSearchItem())
    }

    @Test
    fun saveColumnsReplacesOldRowsAndPersistsFlags() {
        CodegenTestSupport.executeSql(
            ds,
            """INSERT INTO "code_gen_column" ("id", "name", "object_name") VALUES ('stale', 'old_col', 'svc_demo')""",
        )
        val columnInfos = listOf(
            ColumnInfo().apply {
                setName("name")
                setCustomComment("custom")
                setSearchItem(true)
                setListItem(true)
                setEditItem(true)
                setDetailItem(true)
                setCacheItem(true)
            },
            ColumnInfo().apply { setName("built_in") },
        )

        assertTrue(CodeGenColumnService.saveColumns("svc_demo", columnInfos))

        val saved = CodeGenColumnDaoRows()
        assertEquals(setOf("name", "built_in"), saved.names)
        assertEquals("custom", saved.commentOf("name"))
        assertTrue(saved.searchItemOf("name"))
        assertFalse(saved.searchItemOf("built_in"))
    }

    @Test
    fun saveColumnsRejectsNullColumnName() {
        val e = assertFailsWith<IllegalArgumentException> {
            CodeGenColumnService.saveColumns("svc_demo", listOf(ColumnInfo()))
        }
        assertEquals("column name is null", e.message)
    }

    /** Small projection of the code_gen_column rows for svc_demo. */
    private inner class CodeGenColumnDaoRows {
        private val rows = CodegenTestSupport.query(
            ds,
            """SELECT "name", "comment", "search_item" FROM "code_gen_column" WHERE "object_name" = 'svc_demo'"""
        ) { rs -> Triple(rs.getString(1), rs.getString(2), rs.getBoolean(3)) }

        val names get() = rows.map { it.first }.toSet()
        fun commentOf(name: String) = rows.first { it.first == name }.second
        fun searchItemOf(name: String) = rows.first { it.first == name }.third
    }
}
