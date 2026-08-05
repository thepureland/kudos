package io.kudos.tools.codegen.dao

import io.kudos.tools.codegen.CodegenTestSupport
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for CodeGenColumnDao
 *
 * Covers: searchCodeGenColumnMap (hit / miss for object name, map keyed by column name) and
 * deleteCodeGenColumn (only rows of the given object are removed). Runs against an in-memory H2
 * initialized with the module's own DDL and bound through KudosContextHolder, the same lookup
 * path used in production.
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeGenColumnDaoTest {

    private lateinit var ds: DataSource

    @BeforeTest
    fun setUp() {
        ds = CodegenTestSupport.bindNewH2("tools_dao_column")
        CodegenTestSupport.executeSql(
            ds,
            """DELETE FROM "code_gen_column"""",
            """INSERT INTO "code_gen_column" ("id", "name", "object_name", "comment", "search_item")
               VALUES ('c1', 'name', 't_user', 'user name', true)""",
            """INSERT INTO "code_gen_column" ("id", "name", "object_name", "list_item")
               VALUES ('c2', 'age', 't_user', true)""",
            """INSERT INTO "code_gen_column" ("id", "name", "object_name")
               VALUES ('c3', 'title', 't_book')""",
        )
    }

    @Test
    fun searchCodeGenColumnMapReturnsRowsKeyedByColumnName() {
        val map = CodeGenColumnDao.searchCodeGenColumnMap("t_user")
        assertEquals(setOf("name", "age"), map.keys)
        assertEquals("user name", map.getValue("name").comment)
        assertTrue(map.getValue("name").searchItem)
        assertFalse(map.getValue("name").listItem)
        assertTrue(map.getValue("age").listItem)
    }

    @Test
    fun searchCodeGenColumnMapReturnsEmptyForUnknownObject() {
        assertTrue(CodeGenColumnDao.searchCodeGenColumnMap("no_such_table").isEmpty())
    }

    @Test
    fun deleteCodeGenColumnRemovesOnlyGivenObjectRows() {
        CodeGenColumnDao.deleteCodeGenColumn("t_user")
        assertTrue(CodeGenColumnDao.searchCodeGenColumnMap("t_user").isEmpty())
        assertEquals(setOf("title"), CodeGenColumnDao.searchCodeGenColumnMap("t_book").keys)
    }
}
