package io.kudos.tools.codegen.dao

import io.kudos.tools.codegen.CodegenTestSupport
import javax.sql.DataSource
import kotlin.test.*

/**
 * test for CodeGenFileDao
 *
 * Covers: searchCodeGenFileNames returning filenames only for the requested object name and
 * an empty list for unknown objects. Runs against an in-memory H2 bound via KudosContextHolder.
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeGenFileDaoTest {

    private lateinit var ds: DataSource

    @BeforeTest
    fun setUp() {
        ds = CodegenTestSupport.bindNewH2("tools_dao_file")
        CodegenTestSupport.executeSql(
            ds,
            """DELETE FROM "code_gen_file"""",
            """INSERT INTO "code_gen_file" ("id", "filename", "object_name") VALUES ('f1', 'A.kt', 't_user')""",
            """INSERT INTO "code_gen_file" ("id", "filename", "object_name") VALUES ('f2', 'B.kt', 't_user')""",
            """INSERT INTO "code_gen_file" ("id", "filename", "object_name") VALUES ('f3', 'C.kt', 't_book')""",
        )
    }

    @Test
    fun searchCodeGenFileNamesReturnsOnlyMatchingObjectFiles() {
        assertEquals(setOf("A.kt", "B.kt"), CodeGenFileDao.searchCodeGenFileNames("t_user").toSet())
    }

    @Test
    fun searchCodeGenFileNamesReturnsEmptyForUnknownObject() {
        assertTrue(CodeGenFileDao.searchCodeGenFileNames("no_such_table").isEmpty())
    }
}
