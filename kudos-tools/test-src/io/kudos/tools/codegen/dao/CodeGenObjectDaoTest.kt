package io.kudos.tools.codegen.dao

import io.kudos.tools.codegen.CodegenTestSupport
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for CodeGenObjectDao
 *
 * Covers: searchByName found / not-found branches. Runs against an in-memory H2 bound via
 * KudosContextHolder.
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeGenObjectDaoTest {

    private lateinit var ds: DataSource

    @BeforeTest
    fun setUp() {
        ds = CodegenTestSupport.bindNewH2("tools_dao_object")
        CodegenTestSupport.executeSql(
            ds,
            """DELETE FROM "code_gen_object"""",
            """INSERT INTO "code_gen_object" ("id", "name", "comment", "create_user", "gen_count")
               VALUES ('o1', 't_user', 'user table', 'K', 3)""",
        )
    }

    @Test
    fun searchByNameReturnsEntityWhenFound() {
        val entity = CodeGenObjectDao.searchByName("t_user")
        assertNotNull(entity)
        assertEquals("user table", entity.comment)
        assertEquals(3, entity.genCount)
    }

    @Test
    fun searchByNameReturnsNullWhenAbsent() {
        assertNull(CodeGenObjectDao.searchByName("no_such_table"))
    }
}
